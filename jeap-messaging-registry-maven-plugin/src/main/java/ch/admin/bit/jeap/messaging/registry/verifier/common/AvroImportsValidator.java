package ch.admin.bit.jeap.messaging.registry.verifier.common;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class AvroImportsValidator {

    private static final Set<String> RESERVED_KEYWORDS = Set.of(
            "string", "int", "long", "float", "double", "boolean", "bytes", "null",
            "record", "enum", "protocol", "fixed", "union", "namespace", "import"
    );
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+idl\\s+\"([^\"]+)\";\\s*(\\/\\/.*)?$");
    private static final Pattern TYPE_REFERENCE_PATTERN = Pattern.compile("\\b((?:[A-Z][A-Za-z0-9_]*\\.)*[A-Z][A-Za-z0-9_]*)\\b"); // NOSONAR - can lead to a stack overflow for large inputs --> We don't have large inputs
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^\\s*//|^\\s*\\*|^\\s*/\\*|\\*/");

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

    // Method to parse imports from an IDL file
    private Set<String> collectImports(File file) throws IOException {
        Set<String> imports = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = IMPORT_PATTERN.matcher(line.trim());
                if (matcher.find()) {
                    imports.add(matcher.group(1));
                }
            }
        }
        return imports;
    }

    private Set<String> collectTypeReferences(File file) throws IOException {
        Set<String> typeReferences = new LinkedHashSet<>();
        boolean insideRecord = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (COMMENT_PATTERN.matcher(line).find()) {
                    continue;
                }

                if (line.startsWith("record ")) {
                    insideRecord = true;
                }
                if (insideRecord && line.endsWith("}") && !line.contains("{")) {
                    insideRecord = false;
                    continue;
                }

                // Skip lines inside an enum block
                if (!insideRecord) {
                    continue;
                }

                // Process all lines to capture valid types
                Matcher matcher = TYPE_REFERENCE_PATTERN.matcher(line);
                while (matcher.find()) {
                    String potentialType = matcher.group(1);
                    if (!isReservedKeyword(potentialType)) {
                        typeReferences.add(potentialType);
                    }
                }
            }
        }

        return typeReferences;
    }

    private boolean isReservedKeyword(String word) {
        return RESERVED_KEYWORDS.contains(word);
    }

    private boolean isIdlFile(File file) {
        String filePathString = file.toString();
        return filePathString.endsWith(".avdl");
    }
}