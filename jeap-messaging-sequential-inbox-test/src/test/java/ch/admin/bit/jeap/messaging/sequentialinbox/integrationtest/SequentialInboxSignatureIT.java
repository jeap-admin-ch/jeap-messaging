package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequenceInstanceState;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.*;

@DirtiesContext
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.application.name=jme-messaging-receiverpublisher-outbox-service"})
@Slf4j
@ActiveProfiles("test-signing")
@TestPropertySource(properties = "jeap.messaging.sequential-inbox.config-location=classpath:/messaging/jeap-sequential-inbox-for-encryption-and-signature.yml")
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
        assertSequenceState(contextId.toString(), SequenceInstanceState.OPEN);

        JmeSimpleTestEvent jmeSimpleTestEvent = createJmeSimpleTestEvent(contextId, "jme-messaging-receiverpublisher-outbox-service");
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, jmeSimpleTestEvent);

        assertMessageConsumedByListener(event);
        assertSequencedMessageProcessedSuccessfully(event);
        assertSequenceState(contextId.toString(), SequenceInstanceState.CLOSED);
        assertBufferedMessageCount(contextId, 1);
        assertSequenceClosed(contextId);

    }

}
