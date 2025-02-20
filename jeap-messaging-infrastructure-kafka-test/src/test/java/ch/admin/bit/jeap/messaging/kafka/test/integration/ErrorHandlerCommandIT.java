package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureHeaders;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaTestConstants;
import ch.admin.bit.jeap.messaging.kafka.test.TestMessageSender;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeCreateDeclarationCommandBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestCommandConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.test.MockSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = TestConfig.class, properties = {
        "spring.application.name=ErrorHandlerCommandIT"})
@DirtiesContext
class ErrorHandlerCommandIT extends KafkaIntegrationTestBase {
    //Register some listener
    @MockitoBean
    private MessageListener<MessageProcessingFailedEvent> errorEventProcessor;
    @MockitoBean
    private MessageListener<JmeCreateDeclarationCommand> testCommandProcessor;

    //The kafka configuration needed to send invalid messages
    @Autowired
    private KafkaProperties kafkaProperties;

    @Test
    void normalCommand() {
        //Publish a normal command, listener is executed and no error is generated
        AvroMessage message = JmeCreateDeclarationCommandBuilder.create().idempotenceId("idempotenceId").text("message").build();
        sendSync(TestCommandConsumer.TOPIC_NAME, message);
        Mockito.verify(testCommandProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());
        Mockito.verifyNoMoreInteractions(errorEventProcessor);
    }

    @Test
    void generalExceptionWhileCommandHandling() {
        //Command handler will generate an exception
        Mockito.doThrow(new RuntimeException("something")).when(testCommandProcessor).receive(Mockito.any());

        //Publish a normal command, listener is executed and an error is generated
        AvroMessage message = JmeCreateDeclarationCommandBuilder.create().idempotenceId("idempotenceId").text("message").build();
        sendSync(TestCommandConsumer.TOPIC_NAME, message);
        MessageProcessingFailedEvent resultingError = expectError();
        Mockito.verify(testCommandProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());

        //Check there generated error
        Assertions.assertEquals("UNKNOWN_EXCEPTION", resultingError.getReferences().getErrorType().getCode());
        Assertions.assertEquals("UNKNOWN", resultingError.getReferences().getErrorType().getTemporality());
        Assertions.assertTrue(resultingError.getOptionalPayload().isPresent());
        Assertions.assertNull(resultingError.getOptionalPayload().get().getErrorDescription());
        Assertions.assertEquals("java.lang.RuntimeException: something", resultingError.getOptionalPayload().get().getErrorMessage());
        Assertions.assertNotNull(resultingError.getOptionalPayload().get().getOriginalMessage().array());
        Assertions.assertNull(resultingError.getOptionalPayload().get().getOriginalKey());
        Assertions.assertNotNull(resultingError.getOptionalPayload().get().getStackTrace());
    }

    @Test
    void specificExceptionWhileCommandHandling() {
        //Command handler will generate a specific exception
        SpecificException exception = SpecificException.builder()
                .description("desc")
                .errorCode("code")
                .message("msg")
                .temporality(MessageHandlerExceptionInformation.Temporality.TEMPORARY)
                .stackTraceAsString("stack")
                .build();
        Mockito.doThrow(exception).when(testCommandProcessor).receive(Mockito.any());

        //Publish a normal command, listener is executed and an error is generated
        AvroMessage message = JmeCreateDeclarationCommandBuilder.create().idempotenceId("idempotenceId").text("message").build();
        sendSync(TestCommandConsumer.TOPIC_NAME, message);
        MessageProcessingFailedEvent resultingError = expectError();
        Mockito.verify(testCommandProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());

        //Check there generated error
        Assertions.assertEquals("code", resultingError.getReferences().getErrorType().getCode());
        Assertions.assertEquals("TEMPORARY", resultingError.getReferences().getErrorType().getTemporality());
        Assertions.assertTrue(resultingError.getOptionalPayload().isPresent());
        Assertions.assertEquals("desc", resultingError.getOptionalPayload().get().getErrorDescription());
        Assertions.assertEquals("msg", resultingError.getOptionalPayload().get().getErrorMessage());
        Assertions.assertEquals("stack", resultingError.getOptionalPayload().get().getStackTrace());
        Assertions.assertNull(resultingError.getOptionalPayload().get().getOriginalKey());
        Assertions.assertNotNull(resultingError.getOptionalPayload().get().getOriginalMessage());
    }

    @Test
    void testNonDeserializableCommand() {
        //Publish garbage, listener is NOT executed and an error is generated
        publishGarbage();
        MessageProcessingFailedEvent resultingError = expectError();
        Mockito.verifyNoMoreInteractions(testCommandProcessor);

        //Check there generated error
        Assertions.assertEquals("DESERIALIZATION_FAILED", resultingError.getReferences().getErrorType().getCode());
        Assertions.assertEquals("PERMANENT", resultingError.getReferences().getErrorType().getTemporality());
        Assertions.assertTrue(resultingError.getOptionalPayload().isPresent());
        Assertions.assertNull(resultingError.getOptionalPayload().get().getErrorDescription());
        Assertions.assertEquals("java.lang.Exception: Could not deserialize value", resultingError.getOptionalPayload().get().getErrorMessage());
        Assertions.assertArrayEquals(new byte[]{0, 1}, resultingError.getOptionalPayload().get().getOriginalMessage().array());
        Assertions.assertNull(resultingError.getOptionalPayload().get().getOriginalKey());
        Assertions.assertNotNull(resultingError.getOptionalPayload().get().getStackTrace());
    }

    @Test
    void testWrongCommand() {
        //Publish an event instead of an command, listener is NOT executed and an error is generated
        AvroMessage message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").message("message").build();
        sendSync(TestCommandConsumer.TOPIC_NAME, message);
        MessageProcessingFailedEvent resultingError = expectError();
        Mockito.verifyNoMoreInteractions(testCommandProcessor);

        //Check there generated error
        Assertions.assertEquals("WRONG_EVENT_TYPE", resultingError.getReferences().getErrorType().getCode());
        Assertions.assertEquals("PERMANENT", resultingError.getReferences().getErrorType().getTemporality());
        Assertions.assertTrue(resultingError.getOptionalPayload().isPresent());
        Assertions.assertNull(resultingError.getOptionalPayload().get().getErrorDescription());
        Assertions.assertNotNull(resultingError.getOptionalPayload().get().getErrorMessage());
        Assertions.assertNotNull(resultingError.getOptionalPayload().get().getOriginalMessage().array());
        Assertions.assertNull(resultingError.getOptionalPayload().get().getOriginalKey());
        Assertions.assertNotNull(resultingError.getOptionalPayload().get().getStackTrace());
    }

    @Test
    void preserveSignatureHeaders() {
        //Command handler will generate an exception
        Mockito.doThrow(new RuntimeException("something")).when(testCommandProcessor).receive(Mockito.any());

        //Publish a normal command, listener is executed and an error is generated
        AvroMessage message = JmeCreateDeclarationCommandBuilder.create().idempotenceId("idempotenceId").text("message").build();

        TestMessageSender.sendSyncWithHeaders(kafkaTemplate, TestCommandConsumer.TOPIC_NAME, null, message,
                KafkaTestConstants.TEST_PRODUCER_DISABLE_CONTRACT_CHECK_HEADER,
                new RecordHeader("dummy1", new byte[]{1}),
                new RecordHeader(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY, new byte[]{1, 2, 3}),
                new RecordHeader(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY, new byte[]{1, 2, 3, 4}),
                new RecordHeader(SignatureHeaders.SIGNATURE_KEY_HEADER_KEY, new byte[]{1, 2, 3, 4, 5}),
                new RecordHeader("dummy2", new byte[]{1, 2, 3, 4, 5, 6, 7})
        );
        MessageProcessingFailedEvent resultingError = expectError();
        Mockito.verify(testCommandProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());

        Assertions.assertNotNull(resultingError);
        Assertions.assertNotNull(resultingError.getPayload());
        Assertions.assertNotNull(resultingError.getPayload().getFailedMessageMetadata());
        Map<String, ByteBuffer> headers = resultingError.getPayload().getFailedMessageMetadata().getHeaders();
        Assertions.assertNotNull(headers);
        Assertions.assertEquals(3, headers.size());
        Assertions.assertNotNull(headers.get(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY));
        Assertions.assertNotNull(headers.get(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY));
        Assertions.assertNotNull(headers.get(SignatureHeaders.SIGNATURE_KEY_HEADER_KEY));
    }

    private MessageProcessingFailedEvent expectError() {
        ArgumentCaptor<MessageProcessingFailedEvent> captor = ArgumentCaptor.forClass(MessageProcessingFailedEvent.class);
        Mockito.verify(errorEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(captor.capture());
        return captor.getValue();
    }

    private void publishGarbage() {
        Map<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers(KafkaProperties.DEFAULT_CLUSTER));
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, kafkaProperties.getSecurityProtocol(KafkaProperties.DEFAULT_CLUSTER).name);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://none");
        props.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, MockSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MockSerializer.class);
        DefaultKafkaProducerFactory<?, byte[]> producerFactory = new DefaultKafkaProducerFactory<>(props);
        KafkaTemplate<?, byte[]> template = new KafkaTemplate<>(producerFactory);
        template.send(TestCommandConsumer.TOPIC_NAME, new byte[]{0, 1});
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder
    private static class SpecificException extends RuntimeException implements MessageHandlerExceptionInformation {
        private final String errorCode;
        private final String description;
        private final Temporality temporality;
        private final String stackTraceAsString;
        private final String message;
    }
}
