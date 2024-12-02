package ch.admin.bit.jeap.messaging.registry.verifier;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.dto.DescriptorDto;
import ch.admin.bit.jeap.messaging.registry.dto.VersionDto;
import ch.admin.bit.jeap.messaging.registry.helper.MessagingType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FileNotChangedValidator {
    static ValidationResult noSystemDeleted(ValidationContext validationContext) {
        File newDescriptorFolder = descriptorDir(validationContext, false);
        File oldDescriptorFolder = descriptorDir(validationContext, true);

        return Arrays.stream(Objects.requireNonNullElse(oldDescriptorFolder.list(), new String[0]))
                .map(filename -> fileNotDeleted(newDescriptorFolder, filename))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    public static ValidationResult noMessageTypeDeleted(ValidationContext validationContext) {
        File newSystemDir = systemDir(validationContext, false);
        File oldSystemDir = systemDir(validationContext, true);

        if (!oldSystemDir.exists()) {
            //This is a new system, definitely no event deleted
            return ValidationResult.ok();
        }

        newSystemDir = addEventOrCommandFolder(newSystemDir, validationContext);
        oldSystemDir = addEventOrCommandFolder(oldSystemDir, validationContext);

        File finalNewSystemDir = newSystemDir;
        return Arrays.stream(Objects.requireNonNullElse(oldSystemDir.list(), new String[0]))
                .map(filename -> fileNotDeleted(finalNewSystemDir, filename))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    static ValidationResult commonRootDirNotChanged(ValidationContext validationContext) {
        File newCommonRootDir = new File(descriptorDir(validationContext, false), MessageTypeRegistryConstants.COMMON_DIR_NAME);
        File oldCommonRootDir = new File(descriptorDir(validationContext, true), MessageTypeRegistryConstants.COMMON_DIR_NAME);

        if (!oldCommonRootDir.exists() && !newCommonRootDir.exists()) {
            //There is no common dir, this is fine
            return ValidationResult.ok();
        }

        if (!oldCommonRootDir.exists()) {
            String message = "Common dir does not exist in master but does in this branch. You are not allowed to add a common dir on root level";
            return ValidationResult.fail(message);
        }

        if (!newCommonRootDir.exists()) {
            String message = "Common dir does exist in master but does not in this branch. You are not allowed to delete common dir on root level";
            return ValidationResult.fail(message);
        }

        ValidationResult filesCreated = Arrays.stream(Objects.requireNonNullElse(newCommonRootDir.list(), new String[0]))
                .map(filename -> fileExistedBefore(oldCommonRootDir, newCommonRootDir, filename))
                .reduce(ValidationResult.ok(), ValidationResult::merge);

        ValidationResult filesChanged = Arrays.stream(Objects.requireNonNullElse(oldCommonRootDir.list(), new String[0]))
                .map(filename -> fileNotChanged(oldCommonRootDir, newCommonRootDir, filename))
                .reduce(ValidationResult.ok(), ValidationResult::merge);

        return ValidationResult.merge(filesCreated, filesChanged);
    }

    public static ValidationResult commonSystemDirNoFilesChanged(ValidationContext validationContext) {
        File newCommonSystemDir = new File(systemDir(validationContext, false), MessageTypeRegistryConstants.COMMON_DIR_NAME);
        File oldCommonSystemDir = new File(systemDir(validationContext, true), MessageTypeRegistryConstants.COMMON_DIR_NAME);

        if (!oldCommonSystemDir.exists()) {
            //This is a new common dir, definitely no schema deleted
            return ValidationResult.ok();
        }

        if (!newCommonSystemDir.exists()) {
            String message = String.format("Common dir for system %s exist in master but does in this branch." +
                    " You are not allowed to delete a common dir", validationContext.getSystemName());
            return ValidationResult.fail(message);
        }

        return Arrays.stream(Objects.requireNonNullElse(oldCommonSystemDir.list(), new String[0]))
                .map(filename -> fileNotChanged(oldCommonSystemDir, newCommonSystemDir, filename))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    public static ValidationResult noExistingSchemasChanged(ValidationContext validationContext) {
        File newEventDir = eventDir(validationContext, false);
        File oldEventDir = eventDir(validationContext, true);

        if (!oldEventDir.exists()) {
            //This is a new event, definitely no schema deleted
            return ValidationResult.ok();
        }

        return Arrays.stream(Objects.requireNonNullElse(oldEventDir.list(), new String[0]))
                .map(filename -> fileNotChanged(oldEventDir, newEventDir, filename))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private static ValidationResult fileExistedBefore(File oldFolder, File newFolder, String filename) {
        File newFile = new File(newFolder, filename);
        File oldFile = new File(oldFolder, filename);
        if (!oldFile.exists()) {
            String message = String.format("File %s does not exist master but does  exist in this branch. " +
                    "You are not allowed to create this file", newFile.getAbsolutePath());
            return ValidationResult.fail(message);
        }
        return ValidationResult.ok();
    }

    private static ValidationResult fileNotChanged(File oldFolder, File newFolder, String filename) {
        File newFile = new File(newFolder, filename);
        File oldFile = new File(oldFolder, filename);

        if (!newFile.exists()) {
            String message = String.format("File %s exists in master but does not exist in this branch. " +
                    "You are not allowed to delete this file", newFile.getAbsolutePath());
            return ValidationResult.fail(message);
        }

        if (!filename.endsWith("json")) {
            return schemaFileNotChanged(oldFile, newFile);
        } else {
            return alreadyDefinedVersionInDescriptorFileNotChanged(oldFile, newFile);
        }
    }

    private static ValidationResult schemaFileNotChanged(File oldFile, File newFile){
        try {
            if (FileUtils.checksumCRC32(newFile) != FileUtils.checksumCRC32(oldFile)) {
                return ValidationResult.fail(String.format("File %s has changed compared to master. " +
                        "You are not allowed to change this file", newFile.getAbsolutePath()));
            }
        } catch (IOException e) {
            return ValidationResult.fail(String.format("Could not compute checksum: %s", e.getMessage()));
        }
        return ValidationResult.ok();
    }

    private static ValidationResult alreadyDefinedVersionInDescriptorFileNotChanged(File oldFile, File newFile){
        final var objectMapper = new ObjectMapper();

        try {
            final List<VersionDto> oldVersions = objectMapper.readValue(oldFile, DescriptorDto.class).getVersions();
            final List<VersionDto> newVersions = objectMapper.readValue(newFile, DescriptorDto.class).getVersions();

            for (VersionDto version : oldVersions) {
                if (!newVersions.contains(version)) {
                    return ValidationResult.fail(String.format("Already defined version %s cannot be changed", version));
                }
            }

        } catch (IOException e) {
            return ValidationResult.fail(String.format("Could not parse file: %s", e.getMessage()));
        }

        return ValidationResult.ok();
    }

    private static ValidationResult fileNotDeleted(File newFolder, String filename) {
        File newFile = new File(newFolder, filename);

        if (!newFile.exists()) {
            String message = String.format("File %s exists in master but does not exist in this branch. " +
                    "You are not allowed to delete this file", newFile.getAbsolutePath());
            return ValidationResult.fail(message);
        }
        return ValidationResult.ok();
    }

    private static File descriptorDir(ValidationContext validationContext, boolean old) {
        return old ? validationContext.getOldDescriptorDir() : validationContext.getDescriptorDir();
    }

    private static File systemDir(ValidationContext validationContext, boolean old) {
        return new File(descriptorDir(validationContext, old), validationContext.getSystemName());
    }

    private static File eventDir(ValidationContext validationContext, boolean old) {
        File systemDir = systemDir(validationContext, old);
        systemDir = addEventOrCommandFolder(systemDir, validationContext);
        return new File(systemDir, validationContext.getMessageTypeName().toLowerCase());
    }

    private static File addEventOrCommandFolder(File systemDir, ValidationContext validationContext) {
        if (MessagingType.COMMAND.equals(validationContext.getMessagingType())) {
            return new File(systemDir, MessageTypeRegistryConstants.COMMAND_DIR_NAME);
        } else return new File(systemDir, MessageTypeRegistryConstants.EVENT_DIR_NAME);
    }
}
