package ch.admin.bit.jeap.messaging.avro.plugin.validator;

import lombok.Getter;
import org.apache.avro.Schema;


/**
 * In contrast to {@link FieldTypeValidator} this class can check a single
 * fields for the same things as the base class. Furthermore if can also check
 * if required fields are present or not.
 * <p>
 * It is used by {@link SchemaValidator} to validate schemas.
 * It uses a builder like syntax to facilitate usability.
 */
class SingleFieldTypeValidator extends FieldTypeValidator {
    @Getter
    private final String name;
    private final boolean required;

    SingleFieldTypeValidator(RecordValidator recordValidator, final String name, final boolean required) {
        super(recordValidator);
        this.name = name;
        this.required = required;
    }

    @Override
    ValidationResult validate(Schema schema) {
        //First check if the field is present and if so delegate to the base class to check the type
        Schema.Field field = schema.getField(name);
        if (field == null && !required) {
            return ValidationResult.ok();
        }

        if (field == null) {
            String message = String.format("No field '%s' in schema '%s' but it is required",
                    name, schema.getName());
            return ValidationResult.fail(message);
        }
        return super.validate(schema, field);
    }
}
