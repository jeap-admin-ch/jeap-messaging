package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jeap.crypto.api.KeyId;
import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.encryption.CryptoServiceTestConfig;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.createDeclarationCreatedEvent;
import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.randomContextId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

@DirtiesContext
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {TestApp.class, CryptoServiceTestConfig.class}
)
@Slf4j
@ActiveProfiles({"message-encryption-enabled", "key-id-crypto-service"})
class SequentialInboxWithEncryptionIT extends SequentialInboxITBase {

    @Autowired
    protected KeyIdCryptoService keyIdCryptoService;

    @Captor
    ArgumentCaptor<byte[]> plainMessageCaptor;

    @Captor
    ArgumentCaptor<byte[]> encryptedMessageCaptor;

    @Test
    void encryptMessage_and_receiveEncryptedMessage() {

        final KeyId testKeyId = KeyId.of("testKey");

        // given: a test event
        UUID contextId = randomContextId();
        JmeDeclarationCreatedEvent event = createDeclarationCreatedEvent(contextId);

        // when: sending the event
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event);

        // then: assert that the event was consumed by the message listener
        assertMessageConsumedByListener(event);
        assertSequencedMessageProcessedSuccessfully(event);
        assertSequenceOpen(event);
        assertBufferedMessageCount(contextId, 0);

        Mockito.verify(keyIdCryptoService).encrypt(plainMessageCaptor.capture(), eq(testKeyId));
        Mockito.verify(keyIdCryptoService).decrypt(encryptedMessageCaptor.capture());
        byte[] plainMessage = plainMessageCaptor.getValue();
        byte[] encryptedMessage = encryptedMessageCaptor.getValue();
        assertThat(plainMessage).isNotEqualTo(encryptedMessage);
        assertThat(encryptedMessage).isEqualTo(keyIdCryptoService.encrypt(plainMessage, testKeyId));
    }

}
