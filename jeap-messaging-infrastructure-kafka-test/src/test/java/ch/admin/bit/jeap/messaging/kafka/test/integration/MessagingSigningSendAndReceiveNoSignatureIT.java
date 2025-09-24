package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureConfiguration;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@AutoConfigureObservability
@SpringBootTest(classes = {TestConfig.class, SignatureConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=jme-messaging-receiverpublisher-service",
                "management.endpoint.prometheus.access=unrestricted",
                "management.endpoints.web.exposure.include=*",
                "jeap.messaging.kafka.exposeMessageKeyToConsumer=true"
        })
@ActiveProfiles({"test-signing-subscriber"}) // The messages are not signed, in strict mode, that leads to errors (EHS)
@DirtiesContext
class MessagingSigningSendAndReceiveNoSignatureIT extends KafkaIntegrationTestBase {

    @Autowired
    private KafkaProperties kafkaProperties;

    @MockitoBean
    @SuppressWarnings("unused")
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator

    @MockitoBean
    private MessageListener<JmeDeclarationCreatedEvent> jmeEventProcessor;

    @MockitoBean
    private MessageListener<MessageProcessingFailedEvent> errorEventProcessor;

    @MockitoSpyBean
    private DefaultSignatureAuthenticityService signatureAuthenticityService;

    @Test
    void testSignHeaders_sendWithKey() {
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId")
                .serviceName("jme-messaging-receiverpublisher-service")
                .message("gugu1")
                .build();
        BeanReferenceMessageKey messageKey = new BeanReferenceMessageKey("myKey", "myNamespace", "myId");

        AtomicInteger valueCheckCount = new AtomicInteger(0);
        AtomicInteger keyCheckCount = new AtomicInteger(0);

        doAnswer(invocation -> {
            int count = valueCheckCount.incrementAndGet();
            if (count > 1) {
                return null; // The second time it's the message for EHS, we do not check the signature here
            }
            return invocation.callRealMethod(); // Call real method
        }).when(signatureAuthenticityService).checkAuthenticityValue(any(), any(), any());
        // For key
        doAnswer(invocation -> {
            int count = keyCheckCount.incrementAndGet();
            if (count > 1) {
                return null; // The second time it's the message for EHS, we do not check the signature here
            }
            return invocation.callRealMethod();
        }).when(signatureAuthenticityService).checkAuthenticityKey(any(), any());

        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, messageKey, message);

        MessageProcessingFailedEvent messageProcessingFailedEvent = expectError();
        assertNotNull(messageProcessingFailedEvent);

        Mockito.verify(jmeEventProcessor, never()).receive(Mockito.any());
    }

    @Test
    void testSignHeaders_sendWithoutKey() {
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId")
                .serviceName("jme-messaging-receiverpublisher-service")
                .message("gugu2")
                .build();

        AtomicInteger valueCheckCount = new AtomicInteger(0);

        doAnswer(invocation -> {
            int count = valueCheckCount.incrementAndGet();
            if (count > 1) {
                return null; // The second time it's the message for EHS, we do not check the signature here
            }
            return invocation.callRealMethod(); // Call real method
        }).when(signatureAuthenticityService).checkAuthenticityValue(any(), any(), any());

        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message);

        MessageProcessingFailedEvent messageProcessingFailedEvent = expectError();
        assertNotNull(messageProcessingFailedEvent);

        Mockito.verify(jmeEventProcessor, never()).receive(Mockito.any());
    }

    private MessageProcessingFailedEvent expectError() {
        ArgumentCaptor<MessageProcessingFailedEvent> captor = ArgumentCaptor.forClass(MessageProcessingFailedEvent.class);
        Mockito.verify(errorEventProcessor, Mockito.timeout(TEST_TIMEOUT * 3)).receive(captor.capture());
        return captor.getValue();
    }
}
