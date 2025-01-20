package ch.admin.bit.jeap.messaging.registry.verifier.system;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class SystemValidatorTest {
    private final String systemName = "test";
    private final String eventFolder = "event/testtestevent";
    private final String eventName = "TestTestEvent";
    private final String eventDescriptorNoVersionsContent = """
            {
              "eventName": "TestTestEvent",
              "description": "test",
              "definingSystem": "TEST",
              "scope": "public",
              "versions": []
            }
            """;
    private final String eventDescriptorOldStyleContent = """
            {
              "eventName": "TestTestEvent",
              "description": "test",
              "definingSystem": "TEST",
              "scope": "public",
              "versions": [
                  {
                    "version": "1.0.0",
                    "valueSchema": "TestTestEvent_v1.avdl"
                  }
              ]
            }
            """;
    private final String eventDescriptorNewStyleContent = """
            {
              "eventName": "TestTestEvent",
              "description": "test",
              "definingSystem": "TEST",
              "scope": "public",
              "versions": [
                  {
                    "version": "1.0.0",
                    "valueSchema": "TestTestEvent_v1.0.0.avdl"
                  }
              ]
            }
            """;
    private final static File TESTEVENT_RESOURCES_DIR = new File("src/test/resources/valid/descriptor/test/event/testtestevent/");

    @Test
    void uppercaseName(@TempDir File tmpDir) {
        String systemName = "Test";
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .messageTypeName(eventName)
                .systemDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertFalse(result.isValid(), "Uppercase not allowed in system name");
    }

    @Test
    void wrongFileInSystem(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "wrongFile.txt");
        FileUtils.touch(file);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .messageTypeName(eventName)
                .systemDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertFalse(result.isValid(), "Wrong file in dir");
    }

    @Test
    void subDirWithoutEventDescriptor(@TempDir File tmpDir) throws IOException {
        File eventDir = new File(tmpDir, eventFolder);
        FileUtils.forceMkdir(eventDir);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .messageTypeName(eventName)
                .systemDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertFalse(result.isValid(), "Sub dir without event descriptor");
    }

    @Test
    void eventDescriptorButNoSchemas(@TempDir File tmpDir) throws IOException {
        File eventDir = new File(tmpDir, eventFolder);
        FileUtils.forceMkdir(eventDir);
        File eventDescriptor = new File(eventDir, eventName + ".json");
        FileUtils.write(eventDescriptor, eventDescriptorNoVersionsContent);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .messageTypeName(eventName)
                .systemDir(tmpDir)
                .descriptorDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void wrongFileInEvent(@TempDir File tmpDir) throws IOException {
        File eventDir = new File(tmpDir, eventFolder);
        FileUtils.forceMkdir(eventDir);
        File eventDescriptor = new File(eventDir, eventName + ".json");
        FileUtils.write(eventDescriptor, eventDescriptorNoVersionsContent);
        File file = new File(eventDir, "wrongFile.txt");
        FileUtils.touch(file);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .messageTypeName(eventName)
                .systemDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertFalse(result.isValid(), "Wrong file in dir");
    }

    @Test
    void wrongSchemaName(@TempDir File tmpDir) throws IOException {
        File eventDir = new File(tmpDir, eventFolder);
        FileUtils.forceMkdir(eventDir);
        File file = new File(eventDir, "schema_v1.avdl");
        FileUtils.touch(file);
        File eventDescriptor = new File(eventDir, eventName + ".json");
        FileUtils.write(eventDescriptor, eventDescriptorNoVersionsContent);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .messageTypeName(eventName)
                .systemDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertFalse(result.isValid(), "Invalid schema descriptor");
    }

    @Test
    void oldVersionStyleAllowed(@TempDir File tmpDir) throws IOException {
        File eventDir = new File(tmpDir, eventFolder);
        FileUtils.forceMkdir(eventDir);
        File eventDescriptor = new File(eventDir, eventName + ".json");
        FileUtils.write(eventDescriptor, eventDescriptorOldStyleContent);
        File file = new File(eventDir, eventName + "_v1.avdl");
        FileUtils.copyFile(new File(TESTEVENT_RESOURCES_DIR, "TestTestEvent_v1.avdl"), file);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .messageTypeName(eventName)
                .systemDir(tmpDir)
                .descriptorDir(tmpDir)
                .log(new SystemStreamLog())
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void newVersionStyleAllowed(@TempDir File tmpDir) throws IOException {
        File eventDir = new File(tmpDir, eventFolder);
        FileUtils.forceMkdir(eventDir);
        File eventDescriptor = new File(eventDir, eventName + ".json");
        FileUtils.write(eventDescriptor, eventDescriptorNewStyleContent);
        File file = new File(eventDir, eventName + "_v1.0.0.avdl");
        FileUtils.copyFile(new File(TESTEVENT_RESOURCES_DIR, "TestTestEvent_v1.avdl"), file);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .messageTypeName(eventName)
                .systemDir(tmpDir)
                .descriptorDir(tmpDir)
                .log(new SystemStreamLog())
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void otherVersionStyleNotAllowed(@TempDir File tmpDir) throws IOException {
        File eventDir = new File(tmpDir, eventFolder);
        FileUtils.forceMkdir(eventDir);
        File eventDescriptor = new File(eventDir, eventName + ".json");
        FileUtils.write(eventDescriptor, eventDescriptorNoVersionsContent);
        File file = new File(eventDir, eventName + "_v1.0.avdl");
        FileUtils.touch(file);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .messageTypeName(eventName)
                .systemDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertFalse(result.isValid(), "1.0 ist not an allowed version");
    }
}
