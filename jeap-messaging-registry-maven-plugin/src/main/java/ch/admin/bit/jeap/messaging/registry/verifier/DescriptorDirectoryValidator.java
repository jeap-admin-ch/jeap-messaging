package ch.admin.bit.jeap.messaging.registry.verifier;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.system.SystemValidator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DescriptorDirectoryValidator {
    private final File descriptorDirectory;
    private final ValidationContext validationContext;

    public static ValidationResult validate(ValidationContext validationContext) {
        DescriptorDirectoryValidator validator = new DescriptorDirectoryValidator(validationContext.getDescriptorDir(), validationContext);

        ValidationResult directoryValidationResult = validator.checkDirectory();
        if (!directoryValidationResult.isValid()) {
            //If dir is not valid itself, it makes no sense to check its content
            return directoryValidationResult;
        }
        return validator.checkSystems();
    }

    private ValidationResult checkDirectory() {
        String absolutePath = descriptorDirectory.getAbsolutePath();
        if (!descriptorDirectory.exists()) {
            String message = String.format("File '%s' does not exist", absolutePath);
            return ValidationResult.fail(message);
        }
        if (!descriptorDirectory.isDirectory()) {
            String message = String.format("File '%s' is not a directory", absolutePath);
            return ValidationResult.fail(message);
        }
        if (!descriptorDirectory.canRead()) {
            String message = String.format("Directory '%s' is not readable", absolutePath);
            return ValidationResult.fail(message);
        }
        return ValidationResult.merge(
                FileNotChangedValidator.noSystemDeleted(validationContext),
                FileNotChangedValidator.commonRootDirNotChanged(validationContext)
        );
    }

    private ValidationResult checkSystems() {
        String[] systemNames = Objects.requireNonNull(descriptorDirectory.list());
        return Arrays.stream(systemNames)
                .filter(f -> !MessageTypeRegistryConstants.COMMON_DIR_NAME.equals(f))
                .map(this::checkSystem)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private ValidationResult checkSystem(String fileName) {
        ValidationContext subValidationContext = validationContext.toBuilder()
                .systemName(fileName)
                .systemDir(new File(descriptorDirectory, fileName))
                .build();
        return SystemValidator.validate(subValidationContext);
    }
}
