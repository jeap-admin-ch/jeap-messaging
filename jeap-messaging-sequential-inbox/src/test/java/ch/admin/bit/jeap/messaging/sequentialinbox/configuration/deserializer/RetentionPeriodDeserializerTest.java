package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RetentionPeriodDeserializerTest {

    @Test
    void deserialize_durationStyle() throws Exception {
        RetentionPeriodDeserializer deserializer = new RetentionPeriodDeserializer();

        String json = """
                "PT1H30M"
                """;
        Duration duration = deserialize(json, deserializer);

        assertThat(duration)
                .isEqualTo(Duration.ofHours(1).plusMinutes(30));
    }

    @Test
    void deserialize_simpleStyle() throws Exception {
        RetentionPeriodDeserializer deserializer = new RetentionPeriodDeserializer();

        String json = """
                "1h"
                """;
        Duration duration = deserialize(json, deserializer);

        assertThat(duration)
                .isEqualTo(Duration.ofHours(1));
    }

    @Test
    void deserialize_nullValue() throws Exception {
        RetentionPeriodDeserializer deserializer = new RetentionPeriodDeserializer();

        String json = "null";
        Duration duration = deserialize(json, deserializer);

        assertThat(duration).isNull();
    }

    private static Duration deserialize(String json, RetentionPeriodDeserializer deserializer) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonParser parser = new JsonFactory().createParser(json);
        parser.setCodec(objectMapper);
        return deserializer.deserialize(parser, objectMapper.getDeserializationContext());
    }
}
