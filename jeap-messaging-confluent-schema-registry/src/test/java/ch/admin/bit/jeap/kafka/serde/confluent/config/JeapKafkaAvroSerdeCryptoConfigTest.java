package ch.admin.bit.jeap.kafka.serde.confluent.config;

import ch.admin.bit.jeap.crypto.api.CryptoServiceProvider;
import ch.admin.bit.jeap.crypto.api.KeyId;
import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JeapKafkaAvroSerdeCryptoConfigTest {

    @Test
    void testProperConfig() {
        final String encryptedMessageTypeName = "EncryptedMessageType";
        final KeyId keyId = KeyId.of("test-key-id");
        final CryptoServiceProvider cryptoServiceProvider = Mockito.mock(CryptoServiceProvider.class);
        final Map<String, KeyId> messageTypeNameCryptoServiceMap = Map.of(encryptedMessageTypeName, keyId);

        JeapKafkaAvroSerdeCryptoConfig cryptoConfig = new JeapKafkaAvroSerdeCryptoConfig(cryptoServiceProvider, messageTypeNameCryptoServiceMap);

        assertTrue(cryptoConfig.getKeyIdForMessageTypeName(encryptedMessageTypeName).isPresent());
        assertEquals(cryptoConfig.getKeyIdForMessageTypeName(encryptedMessageTypeName).get(), keyId);
        assertTrue(cryptoConfig.getKeyIdForMessageTypeName("PlainMessageType").isEmpty());
    }

    @Test
    void testConstructorMissingMessageTypesKeyReferencesThrowing() {
        String exceptionMessage = assertThrows(NullPointerException.class, () ->
                new JeapKafkaAvroSerdeCryptoConfig(Mockito.mock(CryptoServiceProvider.class), null)).
                getMessage();
        assertEquals("A message type name to key id map has to be provided.", exceptionMessage);
    }
}
