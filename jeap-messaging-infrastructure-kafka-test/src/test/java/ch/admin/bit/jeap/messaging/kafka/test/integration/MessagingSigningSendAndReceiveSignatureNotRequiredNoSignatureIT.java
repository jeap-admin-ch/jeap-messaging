package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureConfiguration;
import ch.admin.bit.jeap.messaging.kafka.signature.subscriber.CertificateAndSignatureVerifier;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.BeanReferenceMessageKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@AutoConfigureObservability
@SpringBootTest(classes = {TestConfig.class, SignatureConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=jme-messaging-receiverpublisher-service",
                "management.endpoint.prometheus.access=unrestricted",
                "management.endpoints.web.exposure.include=*",
                "jeap.messaging.kafka.exposeMessageKeyToConsumer=true",
                "jeap.messaging.authentication.subscriber.require-signature=false" // Signature not required
        })
@ActiveProfiles({"test-signing-subscriber"}) // The messages are not signed, that is ok when not in strict mode
@DirtiesContext
class MessagingSigningSendAndReceiveSignatureNotRequiredNoSignatureIT extends KafkaIntegrationTestBase {

    @Autowired
    private KafkaProperties kafkaProperties;

    @MockitoBean
    @SuppressWarnings("unused")
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator

    @LocalServerPort
    private int localServerPort;

    @MockitoBean
    private MessageListener<JmeDeclarationCreatedEvent> jmeEventProcessor;

    @MockitoSpyBean
    private CertificateAndSignatureVerifier certificateAndSignatureVerifier;

    @Test
    void testSignHeaders_sendWithKey() {
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId")
                .serviceName("jme-messaging-receiverpublisher-service")
                .message("gugu1")
                .build();
        BeanReferenceMessageKey messageKey = new BeanReferenceMessageKey("myKey", "myNamespace", "myId");

        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, messageKey, message);

        Mockito.verify(jmeEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());

        verify(certificateAndSignatureVerifier, never()).verifyValueSignature(any(), any(), any(), any());
        verify(certificateAndSignatureVerifier, never()).verifyKeySignature(any(), any(), any());
    }

    @Test
    void testSignHeaders_sendWithoutKey() {
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId")
                .serviceName("jme-messaging-receiverpublisher-service")
                .message("gugu2")
                .build();

        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message);

        Mockito.verify(jmeEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());

        verify(certificateAndSignatureVerifier, never()).verifyValueSignature(any(), any(), any(), any());
    }
}
