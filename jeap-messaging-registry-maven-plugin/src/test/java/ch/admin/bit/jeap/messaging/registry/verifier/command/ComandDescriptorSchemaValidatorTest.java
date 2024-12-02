package ch.admin.bit.jeap.messaging.registry.verifier.command;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

class ComandDescriptorSchemaValidatorTest {
    @Test
    void validSchema(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, "{\"commandName\":\"TestTestCommand\", \"description\":\"test\",\"definingSystem\":\"TEST\",\"scope\":\"public\"}");
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(file)
                .build();

        ValidationResult result = CommandDescriptorSchemaValidator.validate(validationContext);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void invalidSchema(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, "{\"commandName\":\"TestTestCommand\"}");
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(file)
                .build();

        ValidationResult result = CommandDescriptorSchemaValidator.validate(validationContext);

        Assertions.assertFalse(result.isValid(), "Command descriptor does not confirm to schema");
    }

    @Test
    void notJson(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, "Something");
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(file)
                .build();

        ValidationResult result = CommandDescriptorSchemaValidator.validate(validationContext);

        Assertions.assertFalse(result.isValid(), "Event descriptor is not valid json");
    }
}
