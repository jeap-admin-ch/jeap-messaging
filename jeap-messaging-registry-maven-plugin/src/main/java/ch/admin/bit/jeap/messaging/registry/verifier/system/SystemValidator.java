package ch.admin.bit.jeap.messaging.registry.verifier.system;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.helper.MessagingType;
import ch.admin.bit.jeap.messaging.registry.verifier.FileNotChangedValidator;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.messaging.registry.verifier.command.CommandValidator;
import ch.admin.bit.jeap.messaging.registry.verifier.event.EventValidator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.*;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemValidator {
    private static final List<String> allowedSchemaSuffix = List.of("avdl", "avpr", "avsc");
    private final File systemDirectory;
    private final String messageTypeDirName;
    private final String commandDirName;
    private final String systemName;
    private final List<String> eventTypes = new LinkedList<>();
    private final List<String> commandTypes = new LinkedList<>();

    private final ValidationContext validationContext;

    public static ValidationResult validate(ValidationContext validationContext) {
        SystemValidator systemValidator = new SystemValidator(
                validationContext.getSystemDir(),
                validationContext.getSystemDir().getAbsolutePath() + "/" + MessageTypeRegistryConstants.EVENT_DIR_NAME,
                validationContext.getSystemDir().getAbsolutePath() + "/" + MessageTypeRegistryConstants.COMMAND_DIR_NAME,
                validationContext.getSystemName(),
                validationContext);

        ValidationResult result = ValidationResult.merge(
                systemValidator.checkDirectory(),
                systemValidator.checkSystemName(),
                systemValidator.checkSubDirs(MessagingType.EVENT),
                systemValidator.checkSubDirs(MessagingType.COMMAND),
                systemValidator.checkCommonSystemDir(),
                FileNotChangedValidator.noMessageTypeDeleted(validationContext),
                FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext)
        );

        //If dir is not valid itself, it makes no sense to check its content
        if (!result.isValid()) {
            return result;
        }

        return ValidationResult.merge(
                systemValidator.checkEventDescriptors(),
                systemValidator.checkCommandDescriptors()
        );


    }

    private ValidationResult checkDirectory() {
        String absolutePath = systemDirectory.getAbsolutePath();
        if (!systemDirectory.isDirectory()) {
            String message = String.format("File '%s' is not a directory, but a system has to be", absolutePath);
            return ValidationResult.fail(message);
        }
        if (!systemDirectory.canRead()) {
            String message = String.format("File '%s' is not readable", absolutePath);
            return ValidationResult.fail(message);
        }

        return checkSystemSubDirs(systemDirectory);
    }

    private ValidationResult checkSystemName() {
        String absolutePath = systemDirectory.getAbsolutePath();
        if (!systemName.toLowerCase().equals(systemName)) {
            String message = String.format("System name '%s' in directory '%s' must be lowercase but is not",
                    systemName, absolutePath);
            return ValidationResult.fail(message);
        }
        return ValidationResult.ok();
    }

    private ValidationResult checkSubDirs(MessagingType messageType) {
        String commandEventDirName = "";
        if (messageType == MessagingType.COMMAND) {
            commandTypes.clear();
            commandEventDirName = commandDirName;
        } else if (messageType == MessagingType.EVENT) {
            eventTypes.clear();
            commandEventDirName = messageTypeDirName;
        }

        File subDirsEvent = new File(commandEventDirName);
        String[] subDirs = subDirsEvent.list();
        if (subDirs == null) {
            return ValidationResult.ok();
        }
        String finalCommandEventDirName = commandEventDirName;
        return Arrays.stream(subDirs)
                .filter(f -> !MessageTypeRegistryConstants.COMMON_DIR_NAME.equals(f))
                .map(f -> new File(finalCommandEventDirName, f))
                .map(f -> checkCommandOrEventSubDir(f, messageType))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private ValidationResult checkSystemSubDirs(File sytemDir) {
        String[] systemSubDirs = sytemDir.list();

        if (systemSubDirs == null) {
            return ValidationResult.ok();
        }
        return Arrays.stream(systemSubDirs)
                .filter(f -> !MessageTypeRegistryConstants.COMMON_DIR_NAME.equals(f))
                .map(f -> new File(systemDirectory, f))
                .map(this::checkSystemSubDir)
                .reduce(ValidationResult.ok(), ValidationResult::merge);

    }

    private ValidationResult checkSystemSubDir(File dir) {
        ValidationResult result = checkSubDirIsReadable(dir);
        return result.isValid() ? checkSystemSubdirContent(dir) : result;
    }

    private ValidationResult checkSystemSubdirContent(File systemSubDir) {
        String name = systemSubDir.getName();
        if (MessageTypeRegistryConstants.EVENT_DIR_NAME.equals(name) ||
                MessageTypeRegistryConstants.COMMAND_DIR_NAME.equals(name)) {
            return ValidationResult.ok();
        }

        return ValidationResult.fail("System Directory can only have Subfolder Named 'event' or 'command'");

    }

    private ValidationResult checkCommandOrEventSubDir(File dir, MessagingType messagingType) {
        ValidationResult result = checkSubDirIsReadable(dir);
        return result.isValid() ? checkCommandOrEventSubdirContent(dir, messagingType) : result;
    }

    private ValidationResult checkSubDirIsReadable(File dir) {
        if (!dir.isDirectory()) {
            String message = String.format("File '%s' must be an message type directory but is not",
                    dir.getAbsolutePath());
            return ValidationResult.fail(message);
        }
        if (!dir.canRead()) {
            String message = String.format("File '%s' is not readable",
                    dir.getAbsolutePath());
            return ValidationResult.fail(message);
        }
        if (!dir.getName().equals(dir.getName().toLowerCase())) {
            String message = String.format("Message type directory '%s' must be all lower case",
                    dir.getAbsolutePath());
            return ValidationResult.fail(message);
        }
        return ValidationResult.ok();
    }

    private ValidationResult checkCommandOrEventSubdirContent(File subDir, MessagingType messagingType) {
        //Get the event descriptor and its name
        Optional<String> nameOpt = getCommandOrEventName(subDir);
        if (nameOpt.isEmpty()) {
            String message = String.format("Directory '%s' does not contain a message type descriptor with the correct name",
                    subDir.getAbsolutePath());
            return ValidationResult.fail(message);
        }
        String descriptorName = nameOpt.get();
        if (MessagingType.EVENT.equals(messagingType)) {
            eventTypes.add(descriptorName);
        } else if (MessagingType.COMMAND.equals(messagingType)) {
            commandTypes.add(descriptorName);
        }

        //Check that there are not illegal files
        IOFileFilter notAllowedFileFilter = getNotAllowedFilesFilter(descriptorName);
        String[] notAllowedFiles = Objects.requireNonNull(subDir.list(notAllowedFileFilter));
        return Arrays.stream(notAllowedFiles)
                .map(f -> new File(subDir, f))
                .map(f -> String.format("File '%s' is not allowed, only one descriptor and some schemas",
                        f.getAbsolutePath()))
                .map(ValidationResult::fail)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private Optional<String> getCommandOrEventName(File subDir) {
        //Check if it has a single descriptor
        FilenameFilter jsonFilter = new SuffixFileFilter(".json");
        String[] jsonFileNames = subDir.list(jsonFilter);
        if (jsonFileNames == null) {
            return Optional.empty();
        }
        return Arrays.stream(jsonFileNames)
                .map(FilenameUtils::removeExtension)
                .filter(f -> f.toLowerCase().equals(subDir.getName()))
                .findAny();
    }

    private IOFileFilter getNotAllowedFilesFilter(String descriptorName) {
        IOFileFilter allowedFilesFilter = new OrFileFilter(List.of(new RegexFileFilter(descriptorName + ".json"),
                valueSchemaFilter(descriptorName),
                keySchemaFilter(descriptorName)));
        return new NotFileFilter(allowedFilesFilter);
    }

    private IOFileFilter valueSchemaFilter(String descriptorName) {
        IOFileFilter oldVersion = allowedSchemaSuffix.stream()
                .map(s -> descriptorName + "_v\\d+." + s)
                .map(RegexFileFilter::new)
                .map(f -> (IOFileFilter) f)
                .reduce(new OrFileFilter(), OrFileFilter::new);
        IOFileFilter semanticVersion = allowedSchemaSuffix.stream()
                .map(s -> descriptorName + "_v\\d+\\.\\d+\\.\\d+\\." + s)
                .map(RegexFileFilter::new)
                .map(f -> (IOFileFilter) f)
                .reduce(new OrFileFilter(), OrFileFilter::new);
        return new OrFileFilter(List.of(oldVersion, semanticVersion));
    }

    private IOFileFilter keySchemaFilter(String descriptorName) {
        IOFileFilter oldVersion = allowedSchemaSuffix.stream()
                .map(s -> descriptorName + "_key_v\\d+." + s)
                .map(RegexFileFilter::new)
                .map(f -> (IOFileFilter) f)
                .reduce(new OrFileFilter(), OrFileFilter::new);
        IOFileFilter semanticVersion = allowedSchemaSuffix.stream()
                .map(s -> descriptorName + "_key_v\\d+\\.\\d+\\.\\d+\\." + s)
                .map(RegexFileFilter::new)
                .map(f -> (IOFileFilter) f)
                .reduce(new OrFileFilter(), OrFileFilter::new);
        return new OrFileFilter(List.of(oldVersion, semanticVersion));
    }

    private ValidationResult checkEventDescriptors() {
        return eventTypes.stream()
                .map(this::checkEventDescriptor)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private ValidationResult checkCommandDescriptors() {
        return commandTypes.stream()
                .map(this::checkCommandDescriptor)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private ValidationResult checkEventDescriptor(String eventDescriptorName) {
        File eventDir = new File(messageTypeDirName, eventDescriptorName.toLowerCase());
        ValidationContext subValidationContext = validationContext.toBuilder()
                .messageTypeDirectory(eventDir)
                .descriptorFile(new File(eventDir, eventDescriptorName + ".json"))
                .messageTypeName(eventDescriptorName)
                .messagingType(MessagingType.EVENT)
                .build();
        return EventValidator.validate(subValidationContext);
    }

    private ValidationResult checkCommandDescriptor(String commandDescriptorName) {
        File commandDir = new File(commandDirName, commandDescriptorName.toLowerCase());
        ValidationContext subValidationContext = validationContext.toBuilder()
                .messageTypeDirectory(commandDir)
                .descriptorFile(new File(commandDir, commandDescriptorName + ".json"))
                .messageTypeName(commandDescriptorName)
                .messagingType(MessagingType.COMMAND)
                .build();
        return CommandValidator.validate(subValidationContext);
    }

    private ValidationResult checkCommonSystemDir() {
        return SystemCommonDirValidator.builder()
                .validationContext(validationContext)
                .validate();
    }
}
