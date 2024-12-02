package ch.admin.bit.jeap.messaging.avro.plugin.validator;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SingleFieldTypeValidatorTest {
    @Test
    void optionalNotPresent() {
        FieldTypeValidator target = new SingleFieldTypeValidator(null, "test", false);
        Schema schema = SchemaBuilder.record("schema").fields()
                .endRecord();

        ValidationResult result = target.validate(schema);

        Assertions.assertEquals(ValidationResult.ok(), result);
    }

    @Test
    void requiredNotPresent() {
        FieldTypeValidator target = new SingleFieldTypeValidator(null, "test", true);
        Schema schema = SchemaBuilder.record("schema").fields()
                .endRecord();

        ValidationResult result = target.validate(schema);

        Assertions.assertFalse(result.isValid());
    }

    @Test
    void requiredPresentAndTypeOk() {
        FieldTypeValidator target = new SingleFieldTypeValidator(null, "test", true);
        target.type("string");
        Schema schema = SchemaBuilder.record("schema").fields()
                .name("test").type().stringBuilder().endString().noDefault()
                .endRecord();

        ValidationResult result = target.validate(schema);

        Assertions.assertEquals(ValidationResult.ok(), result);
    }

    @Test
    void requiredPresentAndTypeNok() {
        FieldTypeValidator target = new SingleFieldTypeValidator(null, "test", true);
        target.type("string");
        Schema schema = SchemaBuilder.record("schema").fields()
                .name("test").type().intBuilder().endInt().noDefault()
                .endRecord();

        ValidationResult result = target.validate(schema);

        Assertions.assertFalse(result.isValid());
    }

    @Test
    void otherFieldViolation() {
        FieldTypeValidator target = new SingleFieldTypeValidator(null, "test", true);
        target.type("string");
        Schema schema = SchemaBuilder.record("schema").fields()
                .name("test").type().stringBuilder().endString().noDefault()
                .name("test2").type().intBuilder().endInt().noDefault()
                .endRecord();

        ValidationResult result = target.validate(schema);

        Assertions.assertEquals(ValidationResult.ok(), result);
    }
}
