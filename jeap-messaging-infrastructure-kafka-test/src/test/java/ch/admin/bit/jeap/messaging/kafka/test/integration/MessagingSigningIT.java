package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureConfiguration;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.MessagingMessageConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.MessagingMessageListener;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.BeanReferenceMessageKey;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
@AutoConfigureObservability
@SpringBootTest(classes = {TestConfig.class, SignatureConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=jme-messaging-receiverpublisher-service",
                "management.endpoint.prometheus.access=unrestricted",
                "management.endpoints.web.exposure.include=*"
        })
@ActiveProfiles("test-signing-publisher")
@Import(MessagingMessageConsumer.class)
@DirtiesContext
public class MessagingSigningIT extends KafkaIntegrationTestBase {

    @Autowired
    private KafkaProperties kafkaProperties;

    @MockitoBean
    @SuppressWarnings("unused")
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator

    @LocalServerPort
    private int localServerPort;

    //Register a Messaging Message listener, we need this to verify the message headers
    @MockitoBean
    @SuppressWarnings("unused")
    private MessagingMessageListener messagingMessageListener;

    @Test
    void testSignHeaders_sendWithKey() {
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId").message("gugu1")
                .build();
        BeanReferenceMessageKey messageKey = new BeanReferenceMessageKey("myKey", "myNamespace", "myId");

        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, messageKey, message);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(messagingMessageListener, Mockito.timeout(TEST_TIMEOUT)).receive(captor.capture());
        Message event = captor.getValue();
        MessageHeaders headers = event.getHeaders();

        assertNotNull(headers.get("jeap-cert"));
        assertNotNull(headers.get("jeap-sign"));
        assertNotNull(headers.get("jeap-sign-key"));
    }

    @Test
    void testSignHeaders_sendWithoutKey() {
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId").message("gugu2")
                .build();
        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(messagingMessageListener, Mockito.timeout(TEST_TIMEOUT)).receive(captor.capture());
        Message event = captor.getValue();
        MessageHeaders headers = event.getHeaders();

        assertNotNull(headers.get("jeap-cert"));
        assertNotNull(headers.get("jeap-sign"));
        assertNull(headers.get("jeap-sign-key"));
    }

    @Test
    void testMessagingMetrics() {
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId").message("gugu3")
                .build();
        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message);

        Mockito.verify(messagingMessageListener, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());

        final String metrics = RestAssured.given().port(localServerPort).get("/actuator/prometheus").getBody().asString();
        assertMessagingMetricsCreated(metrics);
    }

    private void assertMessagingMetricsCreated(String metrics) {
        String bootstrapServers = kafkaProperties.getBootstrapServers(KafkaProperties.DEFAULT_CLUSTER);
        assertThat(metrics).contains(
                "jeap_messaging_signature_certificate_days_remaining{application=\"jme-messaging-receiverpublisher-service\"");
        assertThat(metrics).contains(
                "jeap_messaging_total{application=\"jme-messaging-receiverpublisher-service\",bootstrapservers=\"" + bootstrapServers + "\",message=\"JmeDeclarationCreatedEvent\",signed=\"1\",topic=\"jme-messaging-declaration-created\",type=\"producer\",version=\"1.4.0\"}");
        assertThat(metrics).contains(
                "jeap_messaging_total{application=\"jme-messaging-receiverpublisher-service\",bootstrapservers=\"" + bootstrapServers + "\",message=\"JmeDeclarationCreatedEvent\",signed=\"1\",topic=\"jme-messaging-declaration-created\",type=\"consumer\",version=\"1.4.0\"}");
    }
}
