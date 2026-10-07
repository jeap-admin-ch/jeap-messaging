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
    private SchemaJson() { }

    static boolean isRecord(String json) {
        try (var reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement parsed = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT || !parsed.isJsonObject()) return false;
            var record = parsed.getAsJsonObject();
            if (!isString(record.get("type")) || !record.get("type").getAsString().equals("record")
                    || !isString(record.get("name")) || record.get("name").getAsString().isBlank()
                    || !record.has("fields") || !record.get("fields").isJsonArray()) return false;
            var names = new HashSet<String>();
            for (var field : record.getAsJsonArray("fields")) {
                if (!field.isJsonObject()) return false;
                var object = field.getAsJsonObject();
                if (!isString(object.get("name")) || object.get("name").getAsString().isBlank()
                        || !names.add(object.get("name").getAsString()) || !object.has("type")
                        || !(isString(object.get("type")) || object.get("type").isJsonObject() || object.get("type").isJsonArray())) return false;
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
