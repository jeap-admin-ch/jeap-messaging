package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureConfiguration;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureHeaders;
import ch.admin.bit.jeap.messaging.kafka.signature.subscriber.DefaultSignatureAuthenticityService;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.BeanReferenceMessageKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {TestConfig.class, SignatureConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.application.name=jme-messaging-receiverpublisher-service"})
@ActiveProfiles({"test-signing-publisher", "test-signing-subscriber-nochain"})
@DirtiesContext
public class MessagingSigningSendAndReceiveErrorIT extends KafkaIntegrationTestBase {


    @MockitoBean
    @SuppressWarnings("unused")
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator

    @MockitoBean
    private MessageListener<MessageProcessingFailedEvent> errorEventProcessor;

    @MockitoBean
    private MessageListener<JmeDeclarationCreatedEvent> jmeEventProcessor;

    @MockitoSpyBean
    private DefaultSignatureAuthenticityService signatureAuthenticityService;

    @Test
    void testSignHeaders_sendWithKey() {
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").serviceName("jme-messaging-receiverpublisher-service").message("gugu1").build();
        BeanReferenceMessageKey messageKey = new BeanReferenceMessageKey("myKey", "myNamespace", "myId");

        // We don't want an authenticity check on the Error Handler
        doNothing().when(signatureAuthenticityService).checkAuthenticityValue(any(MessageProcessingFailedEvent.class), any(), any());

        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, messageKey, message);

        MessageProcessingFailedEvent messageProcessingFailedEvent = expectError();
        assertNotNull(messageProcessingFailedEvent);
        assertNotNull(messageProcessingFailedEvent.getPayload().getFailedMessageMetadata().getHeaders().get(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY));
        assertNotNull(messageProcessingFailedEvent.getPayload().getFailedMessageMetadata().getHeaders().get(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY));
        assertNotNull(messageProcessingFailedEvent.getPayload().getFailedMessageMetadata().getHeaders().get(SignatureHeaders.SIGNATURE_KEY_HEADER_KEY));
        assertEquals("TEMPORARY", messageProcessingFailedEvent.getReferences().getErrorType().getTemporality());

        Mockito.verify(jmeEventProcessor, never()).receive(Mockito.any());
    }

    @Test
    void testSignHeaders_sendWithoutKey() {
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").serviceName("jme-messaging-receiverpublisher-service").message("gugu2").build();

        // We don't want an authenticity check on the Error Handler
        doNothing().when(signatureAuthenticityService).checkAuthenticityValue(any(MessageProcessingFailedEvent.class), any(), any());

        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message);

        MessageProcessingFailedEvent messageProcessingFailedEvent = expectError();
        assertNotNull(messageProcessingFailedEvent);
        assertNotNull(messageProcessingFailedEvent.getPayload().getFailedMessageMetadata().getHeaders().get(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY));
        assertNotNull(messageProcessingFailedEvent.getPayload().getFailedMessageMetadata().getHeaders().get(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY));
        assertNull(messageProcessingFailedEvent.getPayload().getFailedMessageMetadata().getHeaders().get(SignatureHeaders.SIGNATURE_KEY_HEADER_KEY));
        assertEquals("TEMPORARY", messageProcessingFailedEvent.getReferences().getErrorType().getTemporality());

        Mockito.verify(jmeEventProcessor, never()).receive(Mockito.any());
    }

    private MessageProcessingFailedEvent expectError() {
        ArgumentCaptor<MessageProcessingFailedEvent> captor = ArgumentCaptor.forClass(MessageProcessingFailedEvent.class);
        Mockito.verify(errorEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(captor.capture());
        return captor.getValue();
    }
}
