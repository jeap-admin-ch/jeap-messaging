package ch.admin.bit.jeap.messaging.avro.plugin.validator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.avro.Schema;

import java.util.LinkedList;
import java.util.List;

/**
 * This class can check a single record. To do so you can define multiple rules:
 * - With forAll you can define {@link FieldTypeValidator} rules for all fields
 * - With optional or required you can define {@link SingleFieldTypeValidator} for a specific field
 * - With noOtherFields you can define that there are no other fields that the ones defined with optional
 * or required are allowed to be present
 * <p>
 * It is used by {@link SchemaValidator} to validate schemas.
 * It uses a builder like syntax to facilitate usability.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class RecordValidator {
    private final List<FieldTypeValidator> fieldValidators;
    private boolean hasNoOther;

    static RecordValidator create() {
        return new RecordValidator(new LinkedList<>(), false);
    }

    FieldTypeValidator required(String name) {
        SingleFieldTypeValidator fv = new SingleFieldTypeValidator(this, name, true);
        fieldValidators.add(fv);
        return fv;
    }

    FieldTypeValidator optional(@SuppressWarnings("SameParameterValue") String name) {
        SingleFieldTypeValidator fv = new SingleFieldTypeValidator(this, name, false);
        fv.canBeOptional();
        fieldValidators.add(fv);
        return fv;
    }

    FieldTypeValidator forAll() {
        FieldTypeValidator fv = new FieldTypeValidator(this);
        fieldValidators.add(fv);
        return fv;
    }

    RecordValidator noOtherFields() {
        hasNoOther = true;
        return this;
    }

    public ValidationResult validate(Schema schema) {
        return ValidationResult.merge(
                fieldValidators.stream()
                        .map(v -> v.validate(schema))
                        .reduce(ValidationResult.ok(), ValidationResult::merge),
                validateNoOther(schema)
        );
    }

    private ValidationResult validateNoOther(Schema schema) {
        if (!hasNoOther) {
            return ValidationResult.ok();
        }
        //Get a list of names from fields added with required or optional
        List<String> names = fieldValidators.stream()
                .filter(SingleFieldTypeValidator.class::isInstance)
                .map(f -> (SingleFieldTypeValidator) f)
                .map(SingleFieldTypeValidator::getName)
                .toList();
        //Check all the fields if they are on this list
        return schema.getFields().stream()
                .map(Schema.Field::name)
                .filter(name -> !names.contains(name))
                .map(name -> String.format("Field '%s' in schema '%s' is not allowed, only fields %s are.", name, schema.getName(), names))
                .map(ValidationResult::fail)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }
}
