package ch.admin.bit.jeap.messaging.registry.verifier.system;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.IdlFileParser;
import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.apache.avro.Protocol;
import org.apache.avro.compiler.idl.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class SystemCommonDirValidator {
    private final IdlFileParser idlFileParser;

    @Builder(buildMethodName = "validate")
    static ValidationResult validate(ValidationContext validationContext) {
        File commonRootDir = new File(validationContext.getDescriptorDir(), MessageTypeRegistryConstants.COMMON_DIR_NAME);
        File commonSystemDir = new File(validationContext.getSystemDir(), MessageTypeRegistryConstants.COMMON_DIR_NAME);

        if (!commonSystemDir.exists()) {
            return ValidationResult.ok();
        }

        try (ImportClassLoader importClassLoader = new ImportClassLoader(validationContext.getImportClassLoader(), commonRootDir, commonSystemDir)) {
            SystemCommonDirValidator systemCommonDirValidator = new SystemCommonDirValidator(new IdlFileParser(importClassLoader));

            return Arrays.stream(Objects.requireNonNull(commonSystemDir.list()))
                    .map(name -> new File(commonSystemDir, name))
                    .map(systemCommonDirValidator::validateFile)
                    .reduce(ValidationResult.ok(), ValidationResult::merge);
        } catch (IOException e) {
            return ValidationResult.fail("Cannot import files: " + e.getMessage());
        }
    }

    private ValidationResult validateFile(File file) {
        String filename = file.getName();
        if (!filename.endsWith("avdl")) {
            String message = String.format("File %s is not an Avro IDL file, this is not allowed in common dir",
                    file.getAbsolutePath());
            return ValidationResult.fail(message);
        }

        Protocol protocol;
        try {
            protocol = idlFileParser.parseIdlFile(file);
        } catch (IOException | ParseException e) {
            String message = String.format("File %s cannot be read. Is it a valid Avro IDL file? " + e.getMessage(),
                    file.getAbsolutePath());
            return ValidationResult.fail(message);
        }


        String expectedTypeName = filename.substring(0, filename.length() - 5);
        String expectedProtocolName = expectedTypeName + "Protocol";
        String actualProtocolName = protocol.getNamespace() + "." + protocol.getName();
        if (!actualProtocolName.equals(expectedProtocolName)) {
            String message = String.format("File %s contains protocol %s. This is not allowed as it does not correspond to the filename. It should be %s",
                    file.getAbsolutePath(),
                    actualProtocolName,
                    expectedProtocolName);
            return ValidationResult.fail(message);
        }

        boolean containsValidRecord = protocol.getTypes().stream()
                .map(record -> record.getNamespace() + "." + record.getName())
                .anyMatch(expectedTypeName::equals);
        if (!containsValidRecord) {
            String message = String.format("File %s does not contains type %s. This is not allowed as it must contain a type equal to the filename %s",
                    file.getAbsolutePath(),
                    expectedTypeName,
                    filename);
            return ValidationResult.fail(message);
        }
        return ValidationResult.ok();
    }
}
