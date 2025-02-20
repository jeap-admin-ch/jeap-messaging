package ch.admin.bit.jeap.messaging.kafka.test.integration.glue;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedMessageKey;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureConfiguration;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureHeaders;
import ch.admin.bit.jeap.messaging.kafka.signature.subscriber.DefaultSignatureAuthenticityService;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeSimpleTestEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.MessageProcessingFailedEventConsumer;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.BeanReferenceMessageKey;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;

@SpringBootTest(classes = {TestGlueConfig.class, SignatureConfiguration.class},
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
@Import({KafkaSerdeGlueIT.TestConsumerConfig.class, MessageProcessingFailedEventConsumer.class})
@ActiveProfiles({"test-signing-publisher", "test-signing-subscriber-nochain"})
class KafkaSerdeSigningSendAndReceiveGlueErrorIT extends KafkaGlueIntegrationTestBase {

    @Qualifier("aws")
    @Autowired
    protected KafkaTemplate<AvroMessageKey, AvroMessage> awsKafkaTemplate;

    @Qualifier("aws")
    @Autowired
    @SuppressWarnings("unused")
    protected KafkaAdmin awsKafkaAdmin;

    @Autowired
    protected KafkaProperties kafkaProperties;

    @MockitoBean
    private MessageListener<MessageProcessingFailedEvent> errorEventProcessor;

    @MockitoBean
    private MessageListener<JmeDeclarationCreatedEvent> jmeEventProcessor;

    @MockitoSpyBean
    private DefaultSignatureAuthenticityService signatureAuthenticityService;

    static final String MESSAGE_PROCESSING_FAILED_EVENT_AVRO_SCHEMA = MessageProcessingFailedEvent.SCHEMA$.toString().replace("\"", "\\\"");
    static final String MESSAGE_PROCESSING_FAILED_MESSAGE_KEY_AVRO_SCHEMA = MessageProcessingFailedMessageKey.SCHEMA$.toString().replace("\"", "\\\"");
    static final String DECLARATION_CREATED_EVENT_AVRO_SCHEMA = JmeDeclarationCreatedEvent.SCHEMA$.toString().replace("\"", "\\\"");
    static final String JME_SIMPLE_TEST_EVENT_AVRO_SCHEMA = JmeSimpleTestEvent.SCHEMA$.toString().replace("\"", "\\\"");
    static final String JME_BEAN_REFERENCE_MESSAGE_KEY_AVRO_SCHEMA = BeanReferenceMessageKey.SCHEMA$.toString().replace("\"", "\\\"");


    @Test
    void testSignHeaders_sendWithKey() {
        UUID simpleTestEventVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(simpleTestEventVersionId, "some-other-topic-JmeSimpleTestEvent");
        stubGetSchemaVersionResponse(simpleTestEventVersionId, JME_SIMPLE_TEST_EVENT_AVRO_SCHEMA);

        UUID beanReferenceMessageKeyVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(beanReferenceMessageKeyVersionId, "some-other-topic-BeanReferenceMessageKey-key");
        stubGetSchemaVersionResponse(beanReferenceMessageKeyVersionId, JME_BEAN_REFERENCE_MESSAGE_KEY_AVRO_SCHEMA);

        UUID messageProcessingFailedEventVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(messageProcessingFailedEventVersionId, "errorTopic-MessageProcessingFailedEvent");
        stubGetSchemaVersionResponse(messageProcessingFailedEventVersionId, MESSAGE_PROCESSING_FAILED_EVENT_AVRO_SCHEMA);

        UUID messageProcessingFailedMessageKeyVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(messageProcessingFailedMessageKeyVersionId, "errorTopic-MessageProcessingFailedMessageKey-key");
        stubGetSchemaVersionResponse(messageProcessingFailedMessageKeyVersionId, MESSAGE_PROCESSING_FAILED_MESSAGE_KEY_AVRO_SCHEMA);

        BeanReferenceMessageKey messageKey = new BeanReferenceMessageKey("myKey", "myNamespace", UUID.randomUUID().toString());
        JmeSimpleTestEvent message = JmeSimpleTestEventBuilder.create()
                .idempotenceId("idempotenceId")
                .serviceName("jme-messaging-receiverpublisher-service")
                .message("message")
                .build();

        // We don't want an authenticity check on the Error Handler
        doNothing().when(signatureAuthenticityService).checkAuthenticityValue(any(MessageProcessingFailedEvent.class), any(), any());

        sendSync(awsKafkaTemplate, JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer.OTHER_TOPIC_NAME, messageKey, message);

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
        UUID declarationCreatedEventVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(declarationCreatedEventVersionId, "jme-messaging-declaration-created-JmeDeclarationCreatedEvent");
        stubGetSchemaVersionResponse(declarationCreatedEventVersionId, DECLARATION_CREATED_EVENT_AVRO_SCHEMA);

        UUID messageProcessingFailedEventVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(messageProcessingFailedEventVersionId, "errorTopic-MessageProcessingFailedEvent");
        stubGetSchemaVersionResponse(messageProcessingFailedEventVersionId, MESSAGE_PROCESSING_FAILED_EVENT_AVRO_SCHEMA);

        UUID messageProcessingFailedMessageKeyVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(messageProcessingFailedMessageKeyVersionId, "errorTopic-MessageProcessingFailedMessageKey-key");
        stubGetSchemaVersionResponse(messageProcessingFailedMessageKeyVersionId, MESSAGE_PROCESSING_FAILED_MESSAGE_KEY_AVRO_SCHEMA);

        JmeDeclarationCreatedEvent declarationCreatedEvent = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .message("text")
                .serviceName("jme-messaging-receiverpublisher-service")
                .build();

        // We don't want an authenticity check on the Error Handler
        doNothing().when(signatureAuthenticityService).checkAuthenticityValue(any(MessageProcessingFailedEvent.class), any(), any());

        sendSync(awsKafkaTemplate, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, declarationCreatedEvent);

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
        Mockito.verify(errorEventProcessor, Mockito.timeout(77777)).receive(captor.capture());
        return captor.getValue();
    }
}
