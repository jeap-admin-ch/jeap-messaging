package ch.admin.bit.jeap.messaging.registry.verifier;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class DescriptorDirectoryValidatorTest {

    @Test
    void descriptorDirectoryNotExists(@TempDir File tmpDir) {
        File descriptorDir = new File(tmpDir, "descriptors");
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorDir(descriptorDir)
                .build();

        ValidationResult result = DescriptorDirectoryValidator.validate(validationContext);

        Assertions.assertFalse(result.isValid(), "Must fail if no descriptor directory");
    }

    @Test
    void descriptorDirectoryEmpty(@TempDir File tmpDir) {
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorDir(tmpDir)
                .oldDescriptorDir(tmpDir)
                .build();

        ValidationResult result = DescriptorDirectoryValidator.validate(validationContext);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void fileInDir(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.touch(file);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorDir(tmpDir)
                .oldDescriptorDir(tmpDir)
                .build();

        ValidationResult result = DescriptorDirectoryValidator.validate(validationContext);

        Assertions.assertFalse(result.isValid(), "File in descriptor dir not allowed");
    }

    @Test
    void uppercaseDir(@TempDir File tmpDir) throws IOException {
        File dir = new File(tmpDir, "Test");
        FileUtils.forceMkdir(dir);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorDir(tmpDir)
                .oldDescriptorDir(tmpDir)
                .build();

        ValidationResult result = DescriptorDirectoryValidator.validate(validationContext);

        Assertions.assertFalse(result.isValid(), "Uppercase not allowed in system name");
    }

    @Test
    void valid(@TempDir File tmpDir) throws IOException {
        File dir = new File(tmpDir, "test");
        FileUtils.forceMkdir(dir);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorDir(tmpDir)
                .oldDescriptorDir(tmpDir)
                .build();

        ValidationResult result = DescriptorDirectoryValidator.validate(validationContext);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }
}
