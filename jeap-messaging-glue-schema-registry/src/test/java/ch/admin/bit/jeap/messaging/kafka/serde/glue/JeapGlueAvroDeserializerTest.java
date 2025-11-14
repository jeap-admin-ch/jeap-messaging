package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeapGlueAvroDeserializerTest {

    @Test
    void isLegacyDecryptionActive() {
        Map<String, Object> booleanTrue = Map.of(JeapGlueAvroDeserializer.DECRYPT_MESSAGES_CONFIG, true);
        Map<String, Object> stringTrue = Map.of(JeapGlueAvroDeserializer.DECRYPT_MESSAGES_CONFIG, "true");
        Map<String, Object> stringFalse = Map.of(JeapGlueAvroDeserializer.DECRYPT_MESSAGES_CONFIG, "false");
        Map<String, Object> absent = Map.of();

        assertTrue(JeapGlueAvroDeserializer.isLegacyDecryptionActive(booleanTrue));
        assertTrue(JeapGlueAvroDeserializer.isLegacyDecryptionActive(stringTrue));
        assertFalse(JeapGlueAvroDeserializer.isLegacyDecryptionActive(stringFalse));
        assertFalse(JeapGlueAvroDeserializer.isLegacyDecryptionActive(absent));
    }
}
