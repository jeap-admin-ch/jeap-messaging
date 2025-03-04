package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.boot.convert.DurationStyle;

import java.io.IOException;
import java.time.Duration;

/**
 * Deserializer for {@link Duration} of the sequence instance retention period that supports both ISO format (PT1H30M)
 * and {@link DurationStyle#SIMPLE} (10h).
 */
 class RetentionPeriodDeserializer extends StdDeserializer<Duration> {

     RetentionPeriodDeserializer() {
        super(Duration.class);
    }

    @Override
    public Duration deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (node.isNull()) {
            return null;
        }

        String text = node.asText();
        DurationStyle durationStyle = DurationStyle.detect(text);
        return durationStyle.parse(text);
    }
}
