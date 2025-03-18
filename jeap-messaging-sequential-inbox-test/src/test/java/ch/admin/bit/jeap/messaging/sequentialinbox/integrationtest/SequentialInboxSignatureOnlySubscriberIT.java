package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.ErrorSerializedMessageHolder;
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
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.application.name=jme-messaging-receiverpublisher-outbox-service"})
@Slf4j
@ActiveProfiles("test-signing-only-subscriber")
class SequentialInboxSignatureOnlySubscriberIT extends SequentialInboxITBase {

    @MockitoBean
    @SuppressWarnings("unused")
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator

    @Test
    void doNotSignMessage_and_receiveUnsignedMessage_throwsException() {
        // given: a test event
        UUID contextId = randomContextId();
        JmeDeclarationCreatedEvent event = createDeclarationCreatedEvent(contextId,"jme-messaging-receiverpublisher-outbox-service");

        // when: sending the event
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event);

        // then: assert that the event was not consumed by the message listener but by the error message listener
        messageRecorder.assertCountErrorEvents(1);
        assertThat(((ErrorSerializedMessageHolder) messageRecorder.getRecordedErrorEvents().getFirst()).getCause().getMessage())
                .isEqualTo("ch.admin.bit.jeap.messaging.kafka.signature.exceptions.MessageSignatureValidationException: Received message MessageProcessingFailedEvent from jme-messaging-receiverpublisher-outbox-service without signature certificate but strict mode is enabled. Rejecting message.");
    }

}
