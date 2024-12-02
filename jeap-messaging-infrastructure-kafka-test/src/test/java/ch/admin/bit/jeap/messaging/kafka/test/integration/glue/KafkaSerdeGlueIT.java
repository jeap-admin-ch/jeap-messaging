package ch.admin.bit.jeap.messaging.kafka.test.integration.glue;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeCreateDeclarationCommandBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeSimpleTestEventBuilder;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.BeanReferenceMessageKey;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = TestGlueConfig.class, properties = {
        "spring.application.name=jme-messaging-receiverpublisher-service",
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
@Import({KafkaSerdeGlueIT.TestConsumerConfig.class})
class KafkaSerdeGlueIT extends KafkaGlueIntegrationTestBase {

    @Qualifier("aws")
    @Autowired
    protected KafkaTemplate<AvroMessageKey, AvroMessage> awsKafkaTemplate;

    @Qualifier("aws")
    @Autowired
    protected KafkaAdmin awsKafkaAdmin;

    static final String CREATE_DECLARATION_COMMAND_AVRO_SCHEMA = JmeCreateDeclarationCommand.SCHEMA$.toString().replace("\"", "\\\"");
    static final String DECLARATION_CREATED_EVENT_AVRO_SCHEMA = JmeDeclarationCreatedEvent.SCHEMA$.toString().replace("\"", "\\\"");
    static final String JME_SIMPLE_TEST_EVENT_AVRO_SCHEMA = JmeSimpleTestEvent.SCHEMA$.toString().replace("\"", "\\\"");
    static final String JME_BEAN_REFERENCE_MESSAGE_KEY_AVRO_SCHEMA = BeanReferenceMessageKey.SCHEMA$.toString().replace("\"", "\\\"");

    @Test
    void testConsumeMessage() {
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

        await().until(() -> testConsumer.getCreateDeclarationCommands().size() == 1);
        await().until(() -> testConsumer.getDeclarationCreatedEvents().size() == 1);
    }

    @Test
    void testConsumeMessageWithCustomDeserializationForValue() {
        UUID simpleTestEventVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(simpleTestEventVersionId, "some-topic-JmeSimpleTestEvent");
        stubGetSchemaVersionResponse(simpleTestEventVersionId, JME_SIMPLE_TEST_EVENT_AVRO_SCHEMA);

        JmeSimpleTestEvent message = JmeSimpleTestEventBuilder.create()
                .idempotenceId("idempotenceId")
                .message("consumeOldEvent")
                .build();

        sendSync(awsKafkaTemplate, JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer.TOPIC_NAME, message);

        await().until(() -> testConsumer.getSimpleTestV2Events().size() == 1);
    }

    @Test
    void testConsumeMessageWithCustomDeserializationForValueAndKey() {

        UUID simpleTestEventVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(simpleTestEventVersionId, "some-other-topic-JmeSimpleTestEvent");
        stubGetSchemaVersionResponse(simpleTestEventVersionId, JME_SIMPLE_TEST_EVENT_AVRO_SCHEMA);

        UUID beanReferenceMessageKeyVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(beanReferenceMessageKeyVersionId, "some-other-topic-BeanReferenceMessageKey-key");
        stubGetSchemaVersionResponse(beanReferenceMessageKeyVersionId, JME_BEAN_REFERENCE_MESSAGE_KEY_AVRO_SCHEMA);

        BeanReferenceMessageKey messageKey = new BeanReferenceMessageKey("myKey", "myNamespace", "myId");
        JmeSimpleTestEvent message = JmeSimpleTestEventBuilder.create()
                .idempotenceId("idempotenceId")
                .message("message")
                .build();

        sendSync(awsKafkaTemplate, JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer.OTHER_TOPIC_NAME, messageKey, message);

        await().until(() -> testConsumer.getBeanReferenceMessageKeyV2Keys().size() == 1);
        await().until(() -> testConsumer.getSimpleTestV2EventsFromRecord().size() == 1);
    }

}
