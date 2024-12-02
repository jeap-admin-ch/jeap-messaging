package ch.admin.bit.jeap.messaging.registry.verifier.common;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;

@Builder
@AllArgsConstructor
public class NoDanglingSchemaValidator {

    public static ValidationResult validate(File messageTypeDir, JsonNode messageTypeDescriptor) {

        Set<String> danglingSchemas = findDanglingSchemas(messageTypeDescriptor, messageTypeDir.toPath());
        return danglingSchemas.stream()
                .map(ds -> ValidationResult.fail("Schema file " + ds + " is not referenced"))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private static Set<String> findDanglingSchemas(JsonNode messageTypeDescriptor, Path messageTypeDir) {
        Set<String> referencedSchemas = getSchemaReferencesFromDescriptor(messageTypeDescriptor);
        Set<String> schemaFiles = findAllSchemaFiles(messageTypeDir);
        schemaFiles.removeAll(referencedSchemas);
        return schemaFiles;
    }

    @SneakyThrows
    private static Set<String> findAllSchemaFiles(Path messageTypeDir) {
        try (Stream<Path> pathStream = Files.find(messageTypeDir, 1,
                (path, attr) -> attr.isRegularFile() && path.toString().endsWith(".avdl"))) {
            return pathStream
                    .map(p -> p.getFileName().toString())
                    .collect(toCollection(HashSet::new));
        }
    }

    private static Set<String> getSchemaReferencesFromDescriptor(JsonNode messageTypeDescriptor) {
        JsonNode versions = messageTypeDescriptor.get("versions");
        if (versions == null || !versions.isArray()) {
            return Set.of();
        }

        Set<String> schemaRefs = new HashSet<>();
        for (JsonNode version : versions) {
            String valueSchema = getField(version, "valueSchema");
            String keySchema = getField(version, "keySchema");
            schemaRefs.add(valueSchema);
            schemaRefs.add(keySchema);
        }
        return schemaRefs;
    }

    private static String getField(JsonNode object, String fieldName) {
        JsonNode field = object.get(fieldName);
        if (field != null) {
            return field.textValue();
        }
        return null;
    }
}