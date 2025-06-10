package ch.admin.bit.jeap.messaging.registry.verifier;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.helper.MessagingType;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

class FileNotChangedValidatorTests {
    private File oldCommon;
    private File newCommon;
    private File newSystem;
    private File oldEvent;
    private File newEvent;
    private File oldCommonSystem;
    private File newCommonSystem;
    private ValidationContext validationContext;

    @BeforeEach
    void setup(@TempDir File tmpDir) throws IOException {
        File oldDescriptor = new File(tmpDir, "old");
        FileUtils.forceMkdir(oldDescriptor);
        File newDescriptor = new File(tmpDir, "new");
        FileUtils.forceMkdir(newDescriptor);
        validationContext = ValidationContext.builder()
                .oldDescriptorDir(oldDescriptor)
                .descriptorDir(newDescriptor)
                .systemName("test")
                .messageTypeName("test")
                .messagingType(MessagingType.EVENT)
                .trunkBranchName("test")
                .build();
        oldCommon = new File(oldDescriptor, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        newCommon = new File(newDescriptor, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        File oldSystem = new File(oldDescriptor, "test");
        FileUtils.forceMkdir(oldSystem);
        newSystem = new File(newDescriptor, "test");
        FileUtils.forceMkdir(newSystem);
        oldCommonSystem = new File(oldSystem, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        newCommonSystem = new File(newSystem, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        oldEvent = new File(oldSystem, "event/test");
        FileUtils.forceMkdir(oldEvent);
        newEvent = new File(newSystem, "event/test");
        FileUtils.forceMkdir(newEvent);
    }

    @Test
    void commonFolderNotExisting() {
        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonFolderCreated() throws IOException {
        FileUtils.forceMkdir(newCommon);

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Common folder cannot be created");
    }

    @Test
    void commonFolderDeleted() throws IOException {
        FileUtils.forceMkdir(oldCommon);

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Common folder cannot be deleted");
    }

    @Test
    void commonFoldersEmpty() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonFoldersFileCreated() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);
        FileUtils.write(new File(newCommon, "test"), "hi");

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "File cannot be crated in common folder");
    }

    @Test
    void commonFoldersFileDeleted() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);
        FileUtils.write(new File(oldCommon, "test"), "hi");

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "File cannot be delete in common folder");
    }

    @Test
    void commonFoldersExistingFile() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);
        FileUtils.write(new File(oldCommon, "test"), "hi");
        FileUtils.write(new File(newCommon, "test"), "hi");

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonFoldersFileChanged() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);
        FileUtils.write(new File(oldCommon, "test"), "hi");
        FileUtils.write(new File(newCommon, "test"), "other");

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "File cannot be delete in common folder");
    }

    @Test
    void commonSystemFoldersNotExisting() {
        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonSystemFoldersCreated() throws IOException {
        FileUtils.forceMkdir(newCommonSystem);

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonSystemFoldersDeleted() throws IOException {
        FileUtils.forceMkdir(oldCommonSystem);

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot delete common system folder");
    }

    @Test
    void commonSystemFoldersFileCreated() throws IOException {
        FileUtils.forceMkdir(newCommonSystem);
        FileUtils.forceMkdir(oldCommonSystem);
        FileUtils.write(new File(newCommonSystem, "test"), "other");

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonSystemFoldersFileDeleted() throws IOException {
        FileUtils.forceMkdir(newCommonSystem);
        FileUtils.forceMkdir(oldCommonSystem);
        FileUtils.write(new File(oldCommonSystem, "test"), "other");

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot delete file in common system folder");
    }

    @Test
    void commonSystemFoldersFileChanged() throws IOException {
        FileUtils.forceMkdir(newCommonSystem);
        FileUtils.forceMkdir(oldCommonSystem);
        FileUtils.write(new File(oldCommonSystem, "test"), "hi");
        FileUtils.write(new File(newCommonSystem, "test"), "other");

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot change files in common system folder");
    }

    @Test
    void systemDeleted() throws IOException {
        FileUtils.deleteDirectory(newSystem);

        ValidationResult validationResult = FileNotChangedValidator.noSystemDeleted(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot delete system");
    }

    @Test
    void eventDeleted() throws IOException {
        FileUtils.deleteDirectory(newEvent);

        ValidationResult validationResult = FileNotChangedValidator.noMessageTypeDeleted(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot delete event");
    }

    @Test
    void schemaDeleted() throws IOException {
        FileUtils.write(new File(oldEvent, "test"), "hi");

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot delete schema");
    }

    @Test
    void schemaCreated() throws IOException {
        FileUtils.write(new File(newEvent, "test"), "hi");

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void existingSchema() throws IOException {
        FileUtils.write(new File(newEvent, "test"), "hi");
        FileUtils.write(new File(oldEvent, "test"), "hi");

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void schemaChanged() throws IOException {
        FileUtils.write(new File(newEvent, "test"), "hi");
        FileUtils.write(new File(oldEvent, "test"), "other");

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot change schema");
    }

    @Test
    void descriptorChanged_newVersionAdded_validationOK() throws IOException {
        String oldData = """
                {
                  "versions": [
                    {
                      "version": "1.0.0",
                      "valueSchema": "JmeDecreeCreatedEvent_v1.0.0.avdl"
                    }
                  ]
                }
                """;
        String newData = """
                {
                  "versions": [
                   {
                      "version": "1.0.0",
                      "valueSchema": "JmeDecreeCreatedEvent_v1.0.0.avdl"
                    },
                    {
                      "version": "2.0.0",
                      "valueSchema": "JmeDecreeCreatedEvent_v2.0.0.avdl",
                      "keySchema": "JmeDecreeCreatedEvent_key_v2.avdl"
                    }
                  ]
                }
                """;
        FileUtils.write(new File(newEvent, "test.json"), newData);
        FileUtils.write(new File(oldEvent, "test.json"), oldData);

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);
        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void descriptorChanged_oldVersionModified_validationFailed() throws IOException {
        String oldData = """
                {
                  "versions": [
                    {
                      "version": "1.0.0",
                      "valueSchema": "JmeDecreeCreatedEvent_v1.0.0.avdl"
                    }
                  ]
                }
                """;
        String newData = """
                {
                  "versions": [
                    {
                      "version": "1.0.0",
                      "valueSchema": "JmeDecreeCreatedEvent_v1.0.0.avdl",
                      "keySchema": "JmeDecreeCreatedEvent_key_v1.avdl"
                    }
                  ]
                }
                """;
        FileUtils.write(new File(newEvent, "test.json"), newData);
        FileUtils.write(new File(oldEvent, "test.json"), oldData);

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);
        Assertions.assertFalse(validationResult.isValid(), "Cannot change schema");
    }
}
