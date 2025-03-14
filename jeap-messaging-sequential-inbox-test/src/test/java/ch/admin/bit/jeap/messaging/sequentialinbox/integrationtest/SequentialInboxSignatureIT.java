package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.createDeclarationCreatedEvent;
import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.randomContextId;

@DirtiesContext
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.application.name=jme-messaging-receiverpublisher-outbox-service"})
@Slf4j
@ActiveProfiles("test-signing")
class SequentialInboxSignatureIT extends SequentialInboxITBase {

    @MockitoBean
    @SuppressWarnings("unused")
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator

    @Test
    void signMessage_and_receiveSignedMessage() {
        // given: a test event
        UUID contextId = randomContextId();
        JmeDeclarationCreatedEvent event = createDeclarationCreatedEvent(contextId,"jme-messaging-receiverpublisher-outbox-service");

        // when: sending the event
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event);

        // then: assert that the event was consumed by the message listener
        assertMessageConsumedByListener(event);
        assertSequencedMessageProcessedSuccessfully(event);
        assertSequenceOpen(event);
        assertBufferedMessageCount(contextId, 0);
    }

}
