package ch.admin.bit.jeap.messaging.avro.plugin.validator;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RecordValidatorTest {
    @Test
    void noChecks() {
        Schema schema = SchemaBuilder.record("schema").fields().endRecord();
        RecordValidator target = RecordValidator.create();

        ValidationResult result = target.validate(schema);

        Assertions.assertEquals(ValidationResult.ok(), result);
    }

    @Test
    void noOtherFieldsWithNoFields() {
        Schema schema = SchemaBuilder.record("schema").fields().endRecord();
        RecordValidator target = RecordValidator.create().noOtherFields();

        ValidationResult result = target.validate(schema);

        Assertions.assertEquals(ValidationResult.ok(), result);
    }

    @Test
    void noOtherFieldsWithFields() {
        Schema schema = SchemaBuilder.record("schema").fields().name("test").type("string").noDefault().endRecord();
        RecordValidator target = RecordValidator.create().noOtherFields();

        ValidationResult result = target.validate(schema);

        Assertions.assertFalse(result.isValid());
    }

    @Test
    void handlerFailed() {
        Schema schema = SchemaBuilder.record("schema").fields().name("test").type("string").noDefault().endRecord();
        RecordValidator target = RecordValidator.create().required("test2").end();

        ValidationResult result = target.validate(schema);

        Assertions.assertFalse(result.isValid());
    }
}
