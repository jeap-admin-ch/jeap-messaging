package ch.admin.bit.jeap.messaging.kafka.test.integration.glue;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureConfiguration;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeCreateDeclarationCommandBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeSimpleTestEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.MessagingMessageConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.MessagingMessageListener;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.BeanReferenceMessageKey;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import io.restassured.RestAssured;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = {TestGlueConfig.class, SignatureConfiguration.class, MessagingMessageConsumer.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=jme-messaging-receiverpublisher-service",
                "management.endpoints.web.exposure.include=*",
                "spring.kafka.template.default-topic=default-test-topic",
                "jeap.messaging.kafka.embedded=false",
                "jeap.messaging.kafka.systemName=test",
                "jeap.messaging.kafka.errorTopicName=errorTopic",
                "jeap.messaging.kafka.message-type-encryption-disabled=true",
                "jeap.messaging.kafka.cluster.aws.aws.glue.registryName=testregistry",
                "jeap.messaging.kafka.cluster.aws.aws.glue.region=eu-test-1",
                "jeap.messaging.kafka.cluster.aws.bootstrapServers=${spring.embedded.kafka.brokers}",
                "jeap.messaging.kafka.cluster.aws.securityProtocol=PLAINTEXT",
                "jeap.messaging.kafka.exposeMessageKeyToConsumer=true"
        })
@AutoConfigureObservability
@Import({KafkaSerdeGlueIT.TestConsumerConfig.class, KafkaSerdeSigningGlueIT.MessagingMessageListenerConfig.class})
@ActiveProfiles("test-signing")
class KafkaSerdeSigningGlueIT extends KafkaGlueIntegrationTestBase {

    @LocalServerPort
    private int localServerPort;

    @Qualifier("aws")
    @Autowired
    protected KafkaTemplate<AvroMessageKey, AvroMessage> awsKafkaTemplate;

    @Qualifier("aws")
    @Autowired
    @SuppressWarnings("unused")
    protected KafkaAdmin awsKafkaAdmin;

    @Autowired
    protected KafkaProperties kafkaProperties;

    //Register a Messaging Message listener, we need this to verify the message headers
    @Autowired
    @SuppressWarnings("unused")
    private MessagingMessageTestListener messagingMessageListener;

    static final String CREATE_DECLARATION_COMMAND_AVRO_SCHEMA = JmeCreateDeclarationCommand.SCHEMA$.toString().replace("\"", "\\\"");
    static final String DECLARATION_CREATED_EVENT_AVRO_SCHEMA = JmeDeclarationCreatedEvent.SCHEMA$.toString().replace("\"", "\\\"");
    static final String JME_SIMPLE_TEST_EVENT_AVRO_SCHEMA = JmeSimpleTestEvent.SCHEMA$.toString().replace("\"", "\\\"");
    static final String JME_BEAN_REFERENCE_MESSAGE_KEY_AVRO_SCHEMA = BeanReferenceMessageKey.SCHEMA$.toString().replace("\"", "\\\"");

    @BeforeEach
    void setUp() {
        messagingMessageListener.messages.clear();
    }

    @Test
    void testSignHeaders_sendWithKey() {
        UUID simpleTestEventVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(simpleTestEventVersionId, "some-other-topic-JmeSimpleTestEvent");
        stubGetSchemaVersionResponse(simpleTestEventVersionId, JME_SIMPLE_TEST_EVENT_AVRO_SCHEMA);

        UUID beanReferenceMessageKeyVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(beanReferenceMessageKeyVersionId, "some-other-topic-BeanReferenceMessageKey-key");
        stubGetSchemaVersionResponse(beanReferenceMessageKeyVersionId, JME_BEAN_REFERENCE_MESSAGE_KEY_AVRO_SCHEMA);

        BeanReferenceMessageKey messageKey = new BeanReferenceMessageKey("myKey", "myNamespace", UUID.randomUUID().toString());
        JmeSimpleTestEvent message = JmeSimpleTestEventBuilder.create()
                .idempotenceId("idempotenceId")
                .message("message")
                .build();

        sendSync(awsKafkaTemplate, JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer.OTHER_TOPIC_NAME, messageKey, message);

        await().until(() -> messagingMessageListener.messages.size() == 1);

        Message messagingMessage = messagingMessageListener.messages.getFirst();
        assertNotNull(messagingMessage.getHeaders().get("jeap-cert"));
        assertNotNull(messagingMessage.getHeaders().get("jeap-sign"));
        assertNotNull(messagingMessage.getHeaders().get("jeap-sign-key"));
    }

    @Test
    void testSignHeaders_sendWithoutKey() {
        UUID createDeclarationCommandVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(createDeclarationCommandVersionId, "jme-messaging-create-declaration-JmeCreateDeclarationCommand");
        stubGetSchemaVersionResponse(createDeclarationCommandVersionId, CREATE_DECLARATION_COMMAND_AVRO_SCHEMA);

        UUID declarationCreatedEventVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(declarationCreatedEventVersionId, "jme-messaging-declaration-created-JmeDeclarationCreatedEvent");
        stubGetSchemaVersionResponse(declarationCreatedEventVersionId, DECLARATION_CREATED_EVENT_AVRO_SCHEMA);

        JmeCreateDeclarationCommand createDeclarationCommand = JmeCreateDeclarationCommandBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .text("text")
                .build();
        JmeDeclarationCreatedEvent declarationCreatedEvent = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .message("text")
                .build();

        sendSync(awsKafkaTemplate, JmeCreateDeclarationCommand.TypeRef.DEFAULT_TOPIC, createDeclarationCommand);
        sendSync(awsKafkaTemplate, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, declarationCreatedEvent);

        await().until(() -> messagingMessageListener.messages.size() == 2);

        for (Message message : messagingMessageListener.messages) {
            assertNotNull(message.getHeaders().get("jeap-cert"));
            assertNotNull(message.getHeaders().get("jeap-sign"));
            assertNull(message.getHeaders().get("jeap-sign-key"));
        }
    }

    @Test
    void testMetrics() {
        UUID simpleTestEventVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(simpleTestEventVersionId, "some-other-topic-JmeSimpleTestEvent");
        stubGetSchemaVersionResponse(simpleTestEventVersionId, JME_SIMPLE_TEST_EVENT_AVRO_SCHEMA);

        UUID beanReferenceMessageKeyVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(beanReferenceMessageKeyVersionId, "some-other-topic-BeanReferenceMessageKey-key");
        stubGetSchemaVersionResponse(beanReferenceMessageKeyVersionId, JME_BEAN_REFERENCE_MESSAGE_KEY_AVRO_SCHEMA);

        BeanReferenceMessageKey messageKey = new BeanReferenceMessageKey("myKey", "myNamespace", UUID.randomUUID().toString());
        JmeSimpleTestEvent message = JmeSimpleTestEventBuilder.create()
                .idempotenceId("idempotenceId")
                .message("message")
                .build();

        sendSync(awsKafkaTemplate, JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer.OTHER_TOPIC_NAME, messageKey, message);

        await().until(() -> messagingMessageListener.messages.size() == 1);

        final String metrics = RestAssured.given().port(localServerPort).get("/actuator/prometheus").getBody().asString();
        assertMessagingMetricsCreated(metrics);
    }

    private void assertMessagingMetricsCreated(String metrics) {
        String bootstrapServers = kafkaProperties.getBootstrapServers("aws");
        assertThat(metrics).contains(
                "jeap_messaging_signature_certificate_days_remaining{application=\"jme-messaging-receiverpublisher-service\"");
        assertThat(metrics).contains(
                "jeap_messaging_total{application=\"jme-messaging-receiverpublisher-service\",bootstrapservers=\"" + bootstrapServers + "\",message=\"JmeSimpleTestEvent\",signed=\"1\",topic=\"some-other-topic\",type=\"producer\",version=\"3.0.0\"}");
        assertThat(metrics).contains(
                "jeap_messaging_total{application=\"jme-messaging-receiverpublisher-service\",bootstrapservers=\"" + bootstrapServers + "\",message=\"JmeSimpleTestEvent\",signed=\"0\",topic=\"some-other-topic\",type=\"consumer\",version=\"3.0.0\"}");
    }

    @TestConfiguration
    static class MessagingMessageListenerConfig {
        @Bean
        MessagingMessageListener messagingMessageListener() {
            return new MessagingMessageTestListener();
        }

    }

    @Getter
    static class MessagingMessageTestListener implements MessagingMessageListener {
        private final List<Message<?>> messages = new ArrayList<>();

        @Override
        public void receive(Message<?> message) {
            messages.add(message);
        }
    }

}
