package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.crypto.api.KeyId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

// Message type encryption is disabled by default in the kafka test properties EmbeddedKafkaProperties.
// The following profiles enable the encryption of message types in EmbeddedKafkaProperties and provide a crypto service.
@ActiveProfiles({"message-encryption-enabled", "key-id-crypto-service"})
class SendReceiveEncryptionEnabledIT extends SendReceiveEncryptedITBase {

    @Captor
    ArgumentCaptor<byte[]> plainMessageCaptor;

    @Captor
    ArgumentCaptor<byte[]> encryptedMessageCaptor;

    @Test
    void testSendAndReceiveEncrypted() {
        final KeyId testKeyId = KeyId.of("testKey");

        sendMessage();

        Mockito.verify(keyIdCryptoService).encrypt(plainMessageCaptor.capture(), eq(testKeyId) );
        Mockito.verify(messageProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(messageCaptor.capture());
        Mockito.verify(keyIdCryptoService).decrypt(encryptedMessageCaptor.capture());
        byte[] plainMessage = plainMessageCaptor.getValue();
        byte[] encryptedMessage = encryptedMessageCaptor.getValue();
        assertThat(plainMessage).isNotEqualTo(encryptedMessage);
        assertThat(encryptedMessage).isEqualTo(keyIdCryptoService.encrypt(plainMessage, testKeyId));
        assertThat(messageCaptor.getValue().getPayload().getText()).isEqualTo(MESSAGE_PAYLOAD_TEXT);
    }

}