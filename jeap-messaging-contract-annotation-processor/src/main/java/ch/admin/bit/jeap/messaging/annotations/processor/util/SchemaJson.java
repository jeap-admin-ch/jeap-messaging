package ch.admin.bit.jeap.messaging.annotations.processor.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;

/** Lightweight JSON/record-shape validation without initializing Avro. MCS validates Avro semantics. */
final class SchemaJson {
    private static final String TYPE = "type";
    private static final String NAME = "name";
    private static final String FIELDS = "fields";
    private static final String RECORD = "record";

    private SchemaJson() { }

    static boolean isRecord(String json) {
        try (var reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement parsed = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT || !parsed.isJsonObject()) return false;
            var schemaObject = parsed.getAsJsonObject();
            if (!isString(schemaObject.get(TYPE)) || !schemaObject.get(TYPE).getAsString().equals(RECORD)
                    || !isString(schemaObject.get(NAME)) || schemaObject.get(NAME).getAsString().isBlank()
                    || !schemaObject.has(FIELDS) || !schemaObject.get(FIELDS).isJsonArray()) return false;
            var names = new HashSet<String>();
            for (var field : schemaObject.getAsJsonArray(FIELDS)) {
                if (!field.isJsonObject()) return false;
                var object = field.getAsJsonObject();
                if (!isString(object.get(NAME)) || object.get(NAME).getAsString().isBlank()
                        || !names.add(object.get(NAME).getAsString()) || !object.has(TYPE)
                        || !(isString(object.get(TYPE)) || object.get(TYPE).isJsonObject() || object.get(TYPE).isJsonArray())) return false;
            }
            return true;
        } catch (IOException | RuntimeException invalid) {
            return false;
        }
    }

    private static boolean isString(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }
}
