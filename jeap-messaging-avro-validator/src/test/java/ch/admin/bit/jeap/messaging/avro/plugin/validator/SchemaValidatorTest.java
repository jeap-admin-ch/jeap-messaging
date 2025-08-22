package ch.admin.bit.jeap.messaging.avro.plugin.validator;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

class SchemaValidatorTest {

    @Test
    void ok() throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("unittest/validEvent.avsc");
        Schema schema = new Schema.Parser().parse(in);

        ValidationResult result = SchemaValidator.validate(RecordCollection.of(schema));

        Assertions.assertEquals(ValidationResult.ok(), result);
    }

    @Test
    void okOptionalPayload() throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("unittest/validEventOptionalPayload.avsc");
        Schema schema = new Schema.Parser().parse(in);

        ValidationResult result = SchemaValidator.validate(RecordCollection.of(schema));

        Assertions.assertEquals(ValidationResult.ok(), result);
    }

    @Test
    void okOptionalReferences() throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("unittest/validEventOptionalReferences.avsc");
        Schema schema = new Schema.Parser().parse(in);

        ValidationResult result = SchemaValidator.validate(RecordCollection.of(schema));

        Assertions.assertEquals(ValidationResult.ok(), result);
    }

    @Test
    void okWithVariant() throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("unittest/validEventWithVariant.avsc");
        Schema schema = new Schema.Parser().parse(in);

        ValidationResult result = SchemaValidator.validate(RecordCollection.of(schema));

        Assertions.assertEquals(ValidationResult.ok(), result);
    }

    @Test
    void invalidIdentity() throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("unittest/invalidIdentity.avsc");
        Schema schema = new Schema.Parser().parse(in);

        ValidationResult result = SchemaValidator.validate(RecordCollection.of(schema));

        Assertions.assertFalse(result.isValid());
    }

    @Test
    void invalidPublisher() throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("unittest/invalidPublisher.avsc");
        Schema schema = new Schema.Parser().parse(in);

        ValidationResult result = SchemaValidator.validate(RecordCollection.of(schema));

        Assertions.assertFalse(result.isValid());
    }

    @Test
    void invalidType() throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("unittest/invalidType.avsc");
        Schema schema = new Schema.Parser().parse(in);

        ValidationResult result = SchemaValidator.validate(RecordCollection.of(schema));

        Assertions.assertFalse(result.isValid());
    }

    @Test
    void invalidEvent() throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("unittest/invalidEvent.avsc");
        Schema schema = new Schema.Parser().parse(in);

        ValidationResult result = SchemaValidator.validate(RecordCollection.of(schema));

        Assertions.assertFalse(result.isValid());
    }

    @Test
    void invalidReferences() throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("unittest/invalidReferences.avsc");
        Schema schema = new Schema.Parser().parse(in);

        ValidationResult result = SchemaValidator.validate(RecordCollection.of(schema));

        Assertions.assertFalse(result.isValid());
    }

    @Test
    void invalidReference() throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("unittest/invalidReference.avsc");
        Schema schema = new Schema.Parser().parse(in);

        ValidationResult result = SchemaValidator.validate(RecordCollection.of(schema));

        Assertions.assertFalse(result.isValid());
    }

    @Test
    void messageKeyOk() throws IOException {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("unittest/validMessageKey.avsc");
        Schema schema = new Schema.Parser().parse(in);

        ValidationResult result = SchemaValidator.validate(RecordCollection.of(schema));

        Assertions.assertEquals(ValidationResult.ok(), result);
    }
}
