package ch.admin.bit.jeap.messaging.kafka.test.integration;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

// Message type encryption is disabled by default in the test kafka properties EmbeddedKafkaProperties.
// The following profile provides a crypto service, so encryption would be possible if it had not been disabled.
@ActiveProfiles("key-id-crypto-service")
class SendReceiveEncryptionDisabledIT extends SendReceiveEncryptedITBase {

    @Test
    void testSendAndReceiveUnencrypted() {

        sendMessage();

        Mockito.verifyNoInteractions(keyIdCryptoService);
        Mockito.verifyNoInteractions(keyReferenceCryptoService);
        Mockito.verify(messageProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload().getText()).isEqualTo(MESSAGE_PAYLOAD_TEXT);
    }

}