package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@AutoConfigureObservability
@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.application.name=jme-messaging-subscriber-service",
        "management.endpoint.prometheus.enabled=true",
        "management.endpoints.web.exposure.include=*"})
@DirtiesContext
class MessagingMetricsIT extends KafkaIntegrationTestBase {

    @MockBean
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator

    @LocalServerPort
    private int localServerPort;
    @Autowired
    private KafkaProperties kafkaProperties;

    //Register some event listener
    @MockBean
    private MessageListener<JmeDeclarationCreatedEvent> jmeEventProcessor;

    @Test
    void testMessagingMetrics() {
        //Has consumer contract -> listener must be executed
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId").message("consumeEventWithContract")
                .build();
        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message);

        Mockito.verify(jmeEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());

        final String metrics = RestAssured.given().port(localServerPort).get("/actuator/prometheus").getBody().asString();
        assertMessagingMetricsCreated(metrics);
    }

    private void assertMessagingMetricsCreated(String metrics) {
        String bootstrapServers = kafkaProperties.getBootstrapServers(KafkaProperties.DEFAULT_CLUSTER);
        assertThat(metrics).contains(
                "jeap_messaging_total{application=\"jme-messaging-subscriber-service\",bootstrapservers=\"" + bootstrapServers + "\",message=\"JmeDeclarationCreatedEvent\",topic=\"jme-messaging-declaration-created\",type=\"producer\",version=\"1.4.0\"}");
        assertThat(metrics).contains(
                "jeap_messaging_total{application=\"jme-messaging-subscriber-service\",bootstrapservers=\"" + bootstrapServers + "\",message=\"JmeDeclarationCreatedEvent\",topic=\"jme-messaging-declaration-created\",type=\"consumer\",version=\"1.4.0\"}");
    }
}
