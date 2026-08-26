package ch.admin.bit.jeap.messaging.registry.verifier.event;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

class EventDescriptorSchemaValidatorTest {
    @Test
    void validSchema(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, """
                {"eventName":"TestTestEvent", "description":"test", "definingSystem":"TEST", "scope":"public",
                 "documentationUrl":"https://example.org/event",
                 "versions":[{"version":"1.0.0", "valueSchema":"TestTestEvent_v1.0.0.avdl"}]}
                """);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(file)
                .build();

        ValidationResult result = EventDescriptorSchemaValidator.validate(validationContext);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void invalidSchema(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, "{\"eventName\":\"TestTestEvent\"}");
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(file)
                .build();

        ValidationResult result = EventDescriptorSchemaValidator.validate(validationContext);

        Assertions.assertFalse(result.isValid(), "Event descriptor does not confirm to schema");
    }

    @Test
    void notJson(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, "Something");
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(file)
                .build();

        ValidationResult result = EventDescriptorSchemaValidator.validate(validationContext);

        Assertions.assertFalse(result.isValid(), "Event descriptor is not valid json");
    }

    @Test
    void invalidSharedSchemaFields(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        Files.writeString(file.toPath(), """
                {"eventName":"TestTestEvent", "description":"test", "definingSystem":"TEST", "scope":"public",
                 "documentationUrl":"not-a-url",
                 "versions":[{"version":"invalid", "valueSchema":"not-an-avro-schema"}]}
                """);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(file)
                .build();

        ValidationResult result = EventDescriptorSchemaValidator.validate(validationContext);

        Assertions.assertFalse(result.isValid(), "Referenced common schema constraints must be applied");
        Assertions.assertTrue(result.getErrors().size() >= 3, String.join(",", result.getErrors()));
    }
}
