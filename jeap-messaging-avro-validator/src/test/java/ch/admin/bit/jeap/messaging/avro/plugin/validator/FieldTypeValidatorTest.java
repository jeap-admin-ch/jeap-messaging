package ch.admin.bit.jeap.messaging.avro.plugin.validator;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FieldTypeValidatorTest {
    @Test
    void typeOk() {
        FieldTypeValidator target = new FieldTypeValidator(null);
        target.type("string");
        Schema schema = SchemaBuilder.record("schema").fields()
                .name("test").type().stringBuilder().endString().noDefault()
                .endRecord();

        ValidationResult result = target.validate(schema);

        Assertions.assertEquals(ValidationResult.ok(), result);
    }

    @Test
    void typeNok() {
        FieldTypeValidator target = new FieldTypeValidator(null);
        target.type("testType");
        Schema schema = SchemaBuilder.record("schema").fields()
                .name("test").type().intBuilder().endInt().noDefault()
                .endRecord();

        ValidationResult result = target.validate(schema);

        Assertions.assertFalse(result.isValid());
    }

    @Test
    void namespaceOk() {
        FieldTypeValidator target = new FieldTypeValidator(null);
        target.namespace("testNamespace");
        Schema schema = SchemaBuilder.record("schema").fields()
                .name("test").type().record("test").namespace("testNamespace").fields().endRecord().noDefault()
                .endRecord();

        ValidationResult result = target.validate(schema);

        Assertions.assertEquals(ValidationResult.ok(), result);
    }

    @Test
    void namespaceNok() {
        FieldTypeValidator target = new FieldTypeValidator(null);
        target.namespace("testNamespace");
        Schema schema = SchemaBuilder.record("schema").fields()
                .name("test").type().stringBuilder().endString().noDefault()
                .endRecord();

        ValidationResult result = target.validate(schema);

        Assertions.assertFalse(result.isValid());
    }

    @Test
    void typeSuffixOk() {
        FieldTypeValidator target = new FieldTypeValidator(null);
        target.typeSuffix("ing");
        Schema schema = SchemaBuilder.record("schema").fields()
                .name("test").type().stringBuilder().endString().noDefault()
                .endRecord();

        ValidationResult result = target.validate(schema);

        Assertions.assertEquals(ValidationResult.ok(), result);
    }

    @Test
    void typeSuffixNok() {
        FieldTypeValidator target = new FieldTypeValidator(null);
        target.typeSuffix("ing");
        Schema schema = SchemaBuilder.record("schema").fields()
                .name("test").type().intBuilder().endInt().noDefault()
                .endRecord();

        ValidationResult result = target.validate(schema);

        Assertions.assertFalse(result.isValid());
    }

    @Test
    void logicalTypeOk() {
        Schema timestampMilliType = LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG));
        FieldTypeValidator target = new FieldTypeValidator(null);
        target.logicalType(LogicalTypes.timestampMillis());
        Schema schema = SchemaBuilder.record("schema").fields()
                .name("test").type(timestampMilliType).noDefault()
                .endRecord();

        ValidationResult result = target.validate(schema);

        Assertions.assertEquals(ValidationResult.ok(), result);
    }

    @Test
    void logicalTypeNok() {
        FieldTypeValidator target = new FieldTypeValidator(null);
        target.logicalType(LogicalTypes.timestampMillis());
        Schema schema = SchemaBuilder.record("schema").fields()
                .name("test").type().stringBuilder().endString().noDefault()
                .endRecord();

        ValidationResult result = target.validate(schema);

        Assertions.assertFalse(result.isValid());
    }
}
