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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

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
@ActiveProfiles({"test-signing-publisher", "test-signing-subscriber"})
// The messages are signed, there are certificates in the subscriber, so the signature is validated, even when not in strict mode
@DirtiesContext
class MessagingSigningSendAndReceiveSignatureNotRequiredIT extends KafkaIntegrationTestBase {

    @Autowired
    private KafkaProperties kafkaProperties;

    @MockitoBean
    @SuppressWarnings("unused")
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator

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

        List<Object> authenticityCheckResults = new ArrayList<>();
        List<Object> authenticityKeyCheckResults = new ArrayList<>();

        // For value
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod(); // Call real method
            authenticityCheckResults.add(result);
            return result;
        }).when(certificateAndSignatureVerifier).verifyValueSignature(any(), any(), any(), any());
        // For key
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod(); // Call real method
            authenticityKeyCheckResults.add(result);
            return result;
        }).when(certificateAndSignatureVerifier).verifyKeySignature(any(), any(), any());

        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, messageKey, message);

        Mockito.verify(jmeEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());

        assertEquals(1, authenticityCheckResults.size());
        authenticityCheckResults.forEach(value -> assertTrue((Boolean) value));
        assertEquals(1, authenticityKeyCheckResults.size());
        authenticityKeyCheckResults.forEach(value -> assertTrue((Boolean) value));
    }

    @Test
    void testSignHeaders_sendWithoutKey() {
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId")
                .serviceName("jme-messaging-receiverpublisher-service")
                .message("gugu2")
                .build();

        List<Object> authenticityCheckResults = new ArrayList<>();

        // For value
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod(); // Call real method
            authenticityCheckResults.add(result);
            return result;
        }).when(certificateAndSignatureVerifier).verifyValueSignature(any(), any(), any(), any());

        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message);

        Mockito.verify(jmeEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());

        assertEquals(1, authenticityCheckResults.size());
        authenticityCheckResults.forEach(value -> assertTrue((Boolean) value));
    }

}
