package ch.admin.bit.jeap.messaging.avro.plugin.validator;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.apache.avro.Schema.Type.MAP;

/**
 * This class can check the fields of a record for various things:
 * - type: All fields must be of this type
 * - namespace: All fields must be of a type in this namespace
 * - logicalType: All fields must be of this logical type
 * <p>
 * It is used by {@link SchemaValidator} to validate schemas.
 * It uses a builder like syntax to facilitate usability.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class FieldTypeValidator {
    private final RecordValidator validator;
    private Set<String> types = Set.of();
    private boolean canBeOptional;
    private boolean canBeArray;
    private String namespace;
    private String typeSuffix;
    private LogicalType logicalType;

    FieldTypeValidator type(String type) {
        return anyTypeOf(type);
    }

    FieldTypeValidator anyTypeOf(String... validTypes) {
        this.types = Set.of(validTypes);
        return this;
    }

    FieldTypeValidator canBeOptional() {
        this.canBeOptional = true;
        return this;
    }

    FieldTypeValidator canBeArray() {
        this.canBeArray = true;
        return this;
    }

    FieldTypeValidator namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    FieldTypeValidator typeSuffix(String typeSuffix) {
        this.typeSuffix = typeSuffix;
        return this;
    }

    FieldTypeValidator logicalType(LogicalType logicalType) {
        this.logicalType = logicalType;
        return this;
    }

    RecordValidator end() {
        return validator;
    }

    ValidationResult validate(Schema schema) {
        //We want to check the rules for all fields
        return schema.getFields().stream()
                .map(f -> validate(schema, f))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    final ValidationResult validate(Schema schema, Schema.Field field) {
        //Check each rule individually and merge together the result
        return ValidationResult.merge(
                checkMatchesTypes(schema, field),
                checkNamespace(schema, field),
                checkTypeSuffix(schema, field),
                checkLogicalType(schema, field)
        );
    }

    private ValidationResult checkMatchesTypes(Schema schema, Schema.Field field) {
        if (types.isEmpty()) {
            return ValidationResult.ok();
        }
        if (types.contains(getTypeName(field))) {
            return ValidationResult.ok();
        }
        if (canBeOptional && field.schema().isUnion()) {
            return isValidOptional(schema, field, s -> types.contains(s),
                    String.format("must be (<one of: %s>, null)", types));
        }
        if (canBeArray && field.schema().getType() == Schema.Type.ARRAY) {
            return isValidArray(schema, field, s -> types.contains(s),
                    String.format("must be array<one of: %s>", types.toString()));
        }

        String message = String.format("Field '%s' in schema '%s' has type '%s' but required is one of %s",
                field.name(), schema.getName(), field.schema().getName(), types);
        return ValidationResult.fail(message);

    }

    private String getTypeName(Schema.Field field) {
        String typeName = field.schema().getName();
        if (MAP == field.schema().getType()) {
            String mapValueType = field.schema().getValueType().getName();
            typeName += "<" + mapValueType + ">";
        }
        return typeName;
    }

    private ValidationResult isValidArray(Schema schema, Schema.Field field, Predicate<String> validator, String failMessage) {
        Schema elementType = field.schema().getElementType();
        if (validator.test(elementType.getName())) {
            return ValidationResult.ok();
        } else {
            return ValidationResult.fail(String.format(
                    "Array field '%s' in schema '%s' is invalid: %s", field.name(), schema.getName(), failMessage));
        }
    }

    private ValidationResult isValidOptional(Schema schema, Schema.Field field, Predicate<String> validator, String failMessage) {
        List<Schema> schemaTypes = field.schema().getTypes();
        List<String> nameTypes = schemaTypes.stream()
                .map(s -> (canBeArray && s.getName().equals("array") ? s.getElementType().getName() : s.getName()))
                .toList();
        if (schemaTypes.size() != 2 || !nameTypes.contains("null") || nameTypes.stream().allMatch("null"::equals)) {
            String message = String.format("Field '%s' in schema '%s' is union(%s) but must contain 'null' and one type",
                    field.name(), schema.getName(), String.join(",", nameTypes));
            return ValidationResult.fail(message);
        }

        return nameTypes.stream()
                .filter(t -> !t.equals("null"))
                .filter(t -> !validator.test(t))
                .map(t -> String.format(failMessage, t))
                .map(t -> String.format("Field '%s' in schema '%s' is union(%s) but %s",
                        field.name(), schema.getName(), String.join(",", nameTypes), failMessage))
                .map(ValidationResult::fail)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private ValidationResult checkNamespace(Schema schema, Schema.Field field) {
        if (namespace == null) {
            return ValidationResult.ok();
        }
        try {
            if (field.schema().getNamespace().equals(namespace)) {
                return ValidationResult.ok();
            }
            String message = String.format("Field '%s' in schema '%s' has namespace '%s' but required is '%s'",
                    field.name(), schema.getName(), field.schema().getNamespace(), namespace);
            return ValidationResult.fail(message);
        } catch (AvroRuntimeException e) {
            //This can happen types that cannot have a namespace e.g. primitives
            String message = String.format("Cannot check namespace of field '%s' in schema '%s' but must be '%s'",
                    field.name(), schema.getName(), namespace);
            return ValidationResult.fail(message);
        }
    }

    private ValidationResult checkTypeSuffix(Schema schema, Schema.Field field) {
        if (typeSuffix == null) {
            return ValidationResult.ok();
        }
        if (field.schema().getName().endsWith(typeSuffix)) {
            return ValidationResult.ok();
        }

        if (canBeOptional && field.schema().isUnion()) {
            return isValidOptional(schema, field, s -> s.endsWith(typeSuffix),
                    String.format("type must end with '%s'", typeSuffix));
        }
        if (canBeArray && field.schema().getType() == Schema.Type.ARRAY) {
            return isValidArray(schema, field, s -> s.endsWith(typeSuffix),
                    String.format("array element type must end with '%s'", typeSuffix));
        }

        String message = String.format("Field '%s' in schema '%s' has type '%s' but type must end with '%s'",
                field.name(), schema.getName(), field.schema().getName(), typeSuffix);
        return ValidationResult.fail(message);
    }


    private ValidationResult checkLogicalType(Schema schema, Schema.Field field) {
        if (logicalType == null) {
            return ValidationResult.ok();
        }
        if (field.schema().getLogicalType() == null) {
            String message = String.format("Field '%s' in schema '%s' has no logical type but must have '%s'",
                    field.name(), schema.getName(), logicalType.getName());
            return ValidationResult.fail(message);
        }
        if (field.schema().getLogicalType().equals(logicalType)) {
            return ValidationResult.ok();
        }
        String message = String.format("Field '%s' in schema '%s' has logical type '%s' but must be '%s'",
                field.name(), schema.getName(), field.schema().getLogicalType().getName(), logicalType.getName());
        return ValidationResult.fail(message);
    }
}
