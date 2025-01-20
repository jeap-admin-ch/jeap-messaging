package ch.admin.bit.jeap.messaging.registry.verifier.common;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.IdlFileParser;
import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.RecordCollection;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.SchemaValidator;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.helper.SemanticVersion;
import ch.admin.bit.jeap.messaging.registry.helper.SemanticVersionList;
import ch.admin.bit.jeap.messaging.registry.helper.VersionCompatibility;
import ch.admin.bit.jeap.messaging.registry.helper.VersionCompatibility.Compatibility;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toCollection;

@Builder
@AllArgsConstructor
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class AvroSchemaValidator {
    private final IdlFileParser idlFileParser;
    private final JsonNode messageTypeDescriptor;
    private final Optional<JsonNode> oldMessageTypeDescriptor;
    private final ValidationContext validationContext;
    private Map<String, RecordCollection> recordCollections;
    private ValidationResult schemaValidationResult;

    public static ValidationResult validate(ValidationContext validationContext,
                                            JsonNode messageTypeDescriptor,
                                            Optional<JsonNode> oldMessageTypeDescriptor) {

        try (ImportClassLoader importClassLoader = ImportClassLoaderHelper.generateImportClassLoader(validationContext)) {
            return AvroSchemaValidator.builder()
                    .validationContext(validationContext)
                    .messageTypeDescriptor(messageTypeDescriptor)
                    .oldMessageTypeDescriptor(oldMessageTypeDescriptor)
                    .idlFileParser(new IdlFileParser(importClassLoader))
                    .build()
                    .validate();
        } catch (IOException e) {
            return ValidationResult.fail("Cannot import files: " + e.getMessage());
        }
    }

    private ValidationResult validate() {
        ValidationResult validateValues = validateValues();
        if (validateValues.isValid()) {
            SemanticVersionList semanticVersionList = getVersionsFromDescriptor(messageTypeDescriptor);
            VersionCompatibility versionCompatibility = getCompatibilityModesFromDescriptor(messageTypeDescriptor, semanticVersionList);
            SemanticVersionList oldSemanticVersionList = oldMessageTypeDescriptor
                    .map(AvroSchemaValidator::getVersionsFromDescriptor)
                    .orElseGet(SemanticVersionList::empty);
            validateValues = CompatibilityValidator.validate(validationContext, versionCompatibility,
                    semanticVersionList, oldSemanticVersionList, recordCollections);
        }
        return ValidationResult.merge(validateValues, validateKeys());
    }

    private static SemanticVersionList getVersionsFromDescriptor(JsonNode messageTypeDescriptor) {
        JsonNode versions = messageTypeDescriptor.get("versions");
        if (versions == null) {
            return SemanticVersionList.empty();
        }
        List<SemanticVersion> semanticVersions = StreamSupport.stream(versions.spliterator(), false)
                .map(n -> n.get("version"))
                .map(JsonNode::asText)
                .map(SemanticVersion::parse)
                .toList();
        return new SemanticVersionList(semanticVersions);
    }

    private VersionCompatibility getCompatibilityModesFromDescriptor(JsonNode messageTypeDescriptor, SemanticVersionList semanticVersionList) {
        JsonNode versions = messageTypeDescriptor.get("versions");
        if (versions == null) {
            return VersionCompatibility.empty();
        }
        List<Compatibility> compatibilities = StreamSupport.stream(versions.spliterator(), false)
                .map((JsonNode versionNode) -> Compatibility.parseIfDefined(versionNode, semanticVersionList))
                .filter(Optional::isPresent).map(Optional::get).toList();
        return new VersionCompatibility(compatibilities);
    }

    private ValidationResult validateMessageTypeContainedInSchema(String version, RecordCollection recordCollection) {
        String messageTypeName = validationContext.getMessageTypeName();
        if (messageTypeName == null) {
            return ValidationResult.ok();
        }

        Set<String> schemaTypeNames = recordCollection.getRecords().stream()
                .map(Schema::getName)
                .filter(Objects::nonNull)
                .collect(toCollection(TreeSet::new));
        if (!schemaTypeNames.contains(messageTypeName)) {
            String error = "Message type %s not found in any schema for version %s (check for typos & correct case): %s"
                    .formatted(messageTypeName, version, schemaTypeNames);
            return ValidationResult.fail(error);
        }
        return ValidationResult.ok();
    }

    private ValidationResult validateKeys() {
        if (getVersions().noneMatch(n -> n.has("keySchema"))) {
            //This type has no keys
            return ValidationResult.ok();
        }

        //Validate schemas only if they are readable
        loadSchemas("keySchema");
        return schemaValidationResult;
    }

    private ValidationResult validateValues() {
        loadSchemas("valueSchema");
        if (!schemaValidationResult.isValid()) {
            return schemaValidationResult;
        }
        return recordCollections.entrySet().stream()
                .map(e -> validateMessageType(e.getKey(), e.getValue()))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private void loadSchemas(String type) {
        schemaValidationResult = ValidationResult.ok();
        recordCollections = new HashMap<>();
        getVersions().forEach(versionNode -> {
            if (versionNode.get(type) != null) {
                loadSchema(versionNode.get(type).asText(), versionNode.get("version").asText());
            }
        });
    }

    private void loadSchema(String filename, String version) {
        File schemaFile = getSchemaFile(filename);
        if (schemaFile == null) {
            String message = getMessage(filename);
            schemaValidationResult = ValidationResult.merge(schemaValidationResult, ValidationResult.fail(message));
            return;
        }

        try {
            recordCollections.put(version, getRecordsFromFile(schemaFile));
        } catch (Exception e) {
            String message = String.format("Cannot compile file '%s' (%s): %s", schemaFile.getAbsolutePath(), e.getClass(), e.getMessage());
            schemaValidationResult = ValidationResult.merge(schemaValidationResult, ValidationResult.fail(message));
        }
    }

    private String getMessage(String filename) {
        return String.format("Cannot find schema file '%s' in message type '%s'", filename,
                validationContext.getMessageTypeName());
    }

    private File getSchemaFile(String filename) {
        File commonRootDir = new File(validationContext.getDescriptorDir(), MessageTypeRegistryConstants.COMMON_DIR_NAME);
        File inCommonRootDir = new File(commonRootDir, filename);
        if (inCommonRootDir.exists()) {
            return inCommonRootDir;
        }
        File commonSystemDir = new File(validationContext.getSystemDir(), MessageTypeRegistryConstants.COMMON_DIR_NAME);
        File inCommonSystemDir = new File(commonSystemDir, filename);
        if (inCommonSystemDir.exists()) {
            return inCommonSystemDir;
        }
        File file = new File(validationContext.getMessageTypeDirectory(), filename);
        if (file.exists()) {
            return file;
        }
        return null;
    }

    private RecordCollection getRecordsFromFile(File file) throws Exception {
        if (file.getName().endsWith("avdl")) {
            Protocol protocol = idlFileParser.parseIdlFile(file);
            return RecordCollection.of(protocol);
        } else if (file.getName().endsWith("avpr")) {
            Protocol protocol = Protocol.parse(file);
            return RecordCollection.of(protocol);
        } else if (file.getName().endsWith("avsc")) {
            Schema.Parser parser = new Schema.Parser();
            Schema schema = parser.parse(file);
            return RecordCollection.of(schema);
        } else {
            String message = String.format("File '%s' has an invalid ending", file.getAbsolutePath());
            throw new RuntimeException(message);
        }
    }

    private Stream<JsonNode> getVersions() {
        JsonNode versionsNode = messageTypeDescriptor.get("versions");
        if (versionsNode == null) {
            return Stream.of();
        }
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(versionsNode.elements(), 0), false);
    }

    private ValidationResult validateMessageType(String version, RecordCollection recordCollection) {
        ValidationResult result = SchemaValidator.validate(recordCollection);

        //This does not include the file name, so update the message
        ValidationResult schemaValidationResult = result.getErrors().stream()
                .map(e -> String.format("%s: Schema for version '%s' is not valid: %s", validationContext.getMessageTypeName(), version, e))
                .map(ValidationResult::fail)
                .reduce(ValidationResult.ok(), ValidationResult::merge);

        ValidationResult messageTypeContainedInSchemaResult = validateMessageTypeContainedInSchema(version, recordCollection);

        return ValidationResult.merge(schemaValidationResult, messageTypeContainedInSchemaResult);
    }
}


