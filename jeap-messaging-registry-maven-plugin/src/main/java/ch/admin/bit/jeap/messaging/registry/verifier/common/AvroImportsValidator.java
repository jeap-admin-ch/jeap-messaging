package ch.admin.bit.jeap.messaging.registry.verifier.common;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class AvroImportsValidator {

    private final Log log;
    private final AvroFileInfoCollector fileInfoCollector;
    private final boolean failOnUnusedImports;

    public AvroImportsValidator(Log log, boolean failOnUnusedImports) {
        this(log, failOnUnusedImports, new AvroFileInfoCollector(log));
    }

    AvroImportsValidator(Log log, boolean failOnUnusedImports, AvroFileInfoCollector fileInfoCollector) {
        this.log = log;
        this.failOnUnusedImports = failOnUnusedImports;
        this.fileInfoCollector = fileInfoCollector;
    }

    public static ValidationResult validate(ValidationContext validationContext) {
        AvroImportsValidator importsValidator = new AvroImportsValidator(validationContext.getLog(), validationContext.isFailOnUnusedImports());
        return importsValidator.doValidate(validationContext);
    }

    ValidationResult doValidate(ValidationContext validationContext) {
        ValidationResult result = ValidationResult.ok();
        ImportClassLoader importClassLoader = ImportClassLoaderHelper.generateImportClassLoader(validationContext);

        return getFilesInDirectory(validationContext.getDescriptorFile())
                .map(file -> validate(file, importClassLoader))
                .reduce(result, ValidationResult::merge);
    }

    private Stream<File> getFilesInDirectory(File descriptorFile) {
        File descriptorDir = descriptorFile.getParentFile();
        return Arrays.stream(descriptorDir.listFiles());
    }

    ValidationResult validate(File filePath, ImportClassLoader importClassLoader) {
        // For Schema/Protocol: This step isn’t needed since there are no import statements.
        if (!isIdlFile(filePath)) {
            return ValidationResult.ok();
        }
        log.debug("Checking unused imports for " + filePath);

        Set<String> imports;
        try {
            imports = collectImports(filePath);
        } catch (IOException e) {
            log.error("Error collecting imports for " + filePath, e);
            return ValidationResult.fail("Error collecting imports for " + filePath);
        }

        Set<AvroFileInfo> importedFiles;
        Set<String> referencedTypes;
        try {
            importedFiles = fileInfoCollector.collectAvroFileInfos(imports, importClassLoader);
            referencedTypes = collectTypeReferences(filePath);
        } catch (IOException e) {
            log.error("Error collecting imported files for " + filePath, e);
            return ValidationResult.fail("Error collecting imported files for " + filePath);
        }

        Set<String> unusedImports = determineUnusedImports(importedFiles, referencedTypes);

        if (unusedImports.isEmpty()) {
            return ValidationResult.ok();
        }

        return failOnUnusedImports ?
                ValidationResult.fail("Unused imports in " + filePath + ": " + unusedImports) :
                ValidationResult.ok();
    }

    private Set<String> determineUnusedImports(Set<AvroFileInfo> importedFiles, Set<String> referencedTypes) {
        Set<String> unusedImports = new HashSet<>();
        for (AvroFileInfo importedFile : importedFiles) {
            boolean foundAtLeastOne = false;
            log.debug("Checking imported file: " + importedFile.getImportPath());
            for (String referencedType : referencedTypes) {
                if (importedFile.containsType(referencedType)) {
                    foundAtLeastOne = true;
                    break;
                }
                log.debug(referencedType + " not found in " + importedFile.getImportPath());
            }
            if (!foundAtLeastOne) {
                unusedImports.add(importedFile.getImportPath());
            }
        }
        return unusedImports;
    }

    private Set<String> collectImports(File file) throws IOException {
        return readTokens(file).idlImports();
    }

    private Set<String> collectTypeReferences(File file) throws IOException {
        return readTokens(file).namesUsedInRecords();
    }

    private static IdlTokens readTokens(File file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return IdlTokens.read(inputStream);
        }
    }

    private boolean isIdlFile(File file) {
        String filePathString = file.toString();
        return filePathString.endsWith(".avdl");
    }
}