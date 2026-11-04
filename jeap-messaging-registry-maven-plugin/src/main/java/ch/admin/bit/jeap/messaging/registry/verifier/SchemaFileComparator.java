package ch.admin.bit.jeap.messaging.registry.verifier;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.IdlFileParser;
import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Decides whether a schema file that already exists on the trunk branch has been changed.
 * <p>
 * A published schema must never change, but its comments, documentation and formatting may. Schema files are
 * therefore compared by the schema they define rather than by their bytes: both versions are parsed with the same
 * parser that compiles them, and the resulting Avro schema JSON is compared per named type. Documentation
 * ({@code doc}) and the order in which types are declared or imported are ignored, everything else - names,
 * namespaces, fields and their order, types, defaults, aliases, enum symbols and their order, enum defaults and
 * custom properties - must be identical.
 * <p>
 * {@link Schema#equals(Object)} and {@link Protocol#equals(Object)} cannot be used for this, as they ignore aliases
 * and enum defaults. The Parsing Canonical Form drops defaults and aliases as well.
 */
final class SchemaFileComparator {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final String PROTOCOL_KEY = "protocol declaration";

    enum Result {
        /**
         * The files are byte-identical
         */
        IDENTICAL,
        /**
         * The files differ, but define the same schema (e.g. only comments or formatting changed)
         */
        EQUIVALENT,
        /**
         * The files define different schemas, or the files differ and cannot be compared by their schema
         */
        CHANGED
    }

    record Comparison(Result result, String detail) {
        static Comparison of(Result result) {
            return new Comparison(result, null);
        }
    }

    private SchemaFileComparator() {
    }

    /**
     * @param oldFile             The file as it exists on the trunk branch
     * @param oldImportClassLoader Resolves the imports of the old file against the trunk branch. Only called and
     *                            closed when the files are not byte-identical.
     * @param newFile             The file as it exists in the current branch
     * @param newImportClassLoader Resolves the imports of the new file against the current branch. Only called and
     *                            closed when the files are not byte-identical.
     */
    static Comparison compare(File oldFile, Supplier<ImportClassLoader> oldImportClassLoader,
                              File newFile, Supplier<ImportClassLoader> newImportClassLoader) throws IOException {
        if (Files.mismatch(oldFile.toPath(), newFile.toPath()) == -1) {
            return Comparison.of(Result.IDENTICAL);
        }

        SchemaReader reader = schemaReader(newFile.getName());
        if (reader == null) {
            return Comparison.of(Result.CHANGED);
        }

        SortedMap<String, JsonNode> oldSchema;
        SortedMap<String, JsonNode> newSchema;
        try (ImportClassLoader oldImports = oldImportClassLoader.get();
             ImportClassLoader newImports = newImportClassLoader.get()) {
            oldSchema = readSchema(reader, oldFile, oldImports);
            newSchema = readSchema(reader, newFile, newImports);
        } catch (SchemaNotReadableException e) {
            return new Comparison(Result.CHANGED, e.getMessage());
        }

        List<String> differences = differences(oldSchema, newSchema);
        if (differences.isEmpty()) {
            return Comparison.of(Result.EQUIVALENT);
        }
        return new Comparison(Result.CHANGED, String.join(", ", differences));
    }

    private static SchemaReader schemaReader(String filename) {
        if (filename.endsWith(".avdl")) {
            return (file, imports) -> normalize(new IdlFileParser(imports).parseIdlFile(file));
        }
        if (filename.endsWith(".avpr")) {
            return (file, imports) -> normalize(Protocol.parse(file));
        }
        if (filename.endsWith(".avsc")) {
            return (file, imports) -> normalize(new Schema.Parser().parse(file));
        }
        return null;
    }

    private static SortedMap<String, JsonNode> readSchema(SchemaReader reader, File file, ImportClassLoader imports) {
        try {
            return reader.read(file, imports);
        } catch (Exception e) {
            // Fail closed: a file that cannot be parsed is never considered unchanged
            throw new SchemaNotReadableException("cannot parse %s: %s".formatted(file.getAbsolutePath(), e.getMessage()));
        }
    }

    /**
     * Represents a protocol by its declaration (name, namespace, properties, messages) and by its named types,
     * keyed by full name so that the declaration order of types and imports does not matter.
     */
    private static SortedMap<String, JsonNode> normalize(Protocol protocol) {
        SortedMap<String, JsonNode> normalized = new TreeMap<>();
        ObjectNode declaration = (ObjectNode) JSON_MAPPER.readTree(protocol.toString());
        declaration.remove("types");
        declaration.remove("doc");
        JsonNode messages = declaration.get("messages");
        if (messages != null) {
            messages.forEach(SchemaFileComparator::removeMessageDocs);
        }
        normalized.put(PROTOCOL_KEY, declaration);
        protocol.getTypes().forEach(type -> normalized.put(type.getFullName(), schemaWithoutDocs(type)));
        return normalized;
    }

    private static SortedMap<String, JsonNode> normalize(Schema schema) {
        SortedMap<String, JsonNode> normalized = new TreeMap<>();
        normalized.put(isNamed(schema) ? schema.getFullName() : "schema", schemaWithoutDocs(schema));
        return normalized;
    }

    private static boolean isNamed(Schema schema) {
        return switch (schema.getType()) {
            case RECORD, ENUM, FIXED -> true;
            default -> false;
        };
    }

    private static JsonNode schemaWithoutDocs(Schema schema) {
        JsonNode json = JSON_MAPPER.readTree(schema.toString());
        removeSchemaDocs(json);
        return json;
    }

    /**
     * Removes documentation from a schema JSON. Only the structural positions of a schema are visited, so that
     * default values and custom property values (which may contain a key named "doc") are left untouched.
     */
    private static void removeSchemaDocs(JsonNode schema) {
        if (schema == null) {
            return;
        }
        if (schema.isArray()) {
            // union
            schema.forEach(SchemaFileComparator::removeSchemaDocs);
            return;
        }
        if (!(schema instanceof ObjectNode schemaObject)) {
            return;
        }
        schemaObject.remove("doc");
        removeSchemaDocs(schemaObject.get("type"));
        removeSchemaDocs(schemaObject.get("items"));
        removeSchemaDocs(schemaObject.get("values"));
        removeFieldDocs(schemaObject.get("fields"));
    }

    private static void removeFieldDocs(JsonNode fields) {
        if (fields == null) {
            return;
        }
        for (JsonNode field : fields) {
            if (field instanceof ObjectNode fieldObject) {
                fieldObject.remove("doc");
                removeSchemaDocs(fieldObject.get("type"));
            }
        }
    }

    private static void removeMessageDocs(JsonNode message) {
        if (message instanceof ObjectNode messageObject) {
            messageObject.remove("doc");
            removeFieldDocs(messageObject.get("request"));
            removeSchemaDocs(messageObject.get("response"));
            removeSchemaDocs(messageObject.get("errors"));
        }
    }

    private static List<String> differences(Map<String, JsonNode> oldSchema, Map<String, JsonNode> newSchema) {
        List<String> differences = new ArrayList<>();
        TreeSet<String> names = new TreeSet<>(oldSchema.keySet());
        names.addAll(newSchema.keySet());
        for (String name : names) {
            JsonNode oldType = oldSchema.get(name);
            JsonNode newType = newSchema.get(name);
            if (newType == null) {
                differences.add(name + " removed");
            } else if (oldType == null) {
                differences.add(name + " added");
            } else if (!oldType.equals(newType)) {
                differences.add(name + " changed");
            }
        }
        return differences;
    }

    @FunctionalInterface
    private interface SchemaReader {
        SortedMap<String, JsonNode> read(File file, ImportClassLoader imports) throws Exception;
    }

    private static class SchemaNotReadableException extends RuntimeException {
        SchemaNotReadableException(String message) {
            super(message);
        }
    }
}
