package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaTestConstants;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeCreateDeclarationCommandBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestEventConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestMessageConsumer;
import ch.admin.bit.jeap.test.avro.message.TestMessage;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
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
import org.springframework.kafka.support.SendResult;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = TestConfig.class, properties = {
        "spring.application.name=ErrorHandlerIT"})
@DirtiesContext
class ErrorHandlerIT extends KafkaIntegrationTestBase {
    //Register some event listener
    @MockitoBean
    private MessageListener<MessageProcessingFailedEvent> errorEventProcessor;
    @MockitoBean
    private MessageListener<JmeDeclarationCreatedEvent> testEventProcessor;
    @MockitoBean
    private Consumer<TestMessage> testMessageConsumer;
    @Autowired
    protected KafkaTemplate<Object, Object> kafkaTemplate;

    //The kafka configuration needed to send non domain-event events
    @Autowired
    private KafkaProperties kafkaProperties;

    @Test
    void normalEvent() {
        //Publish a normal event, listener is executed and no error is generated
        AvroMessage message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").message("message").build();
        sendSync(TestEventConsumer.TOPIC_NAME, message);
        Mockito.verify(testEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());
        Mockito.verifyNoMoreInteractions(errorEventProcessor);
    }

    @Test
    void generalExceptionWhileEventHandling() {
        //Event handler will generate an exception
        Mockito.doThrow(new RuntimeException("something")).when(testEventProcessor).receive(Mockito.any());

        //Publish a normal event, listener is executed and an error is generated
        AvroMessage message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").message("message").build();
        sendSync(TestEventConsumer.TOPIC_NAME, message);
        MessageProcessingFailedEvent resultingError = expectError();
        Mockito.verify(testEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());

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
    void generalExceptionWhileEventHandling_avroSpecificRecord() {
        //Event handler will generate an exception
        Mockito.doThrow(new RuntimeException("something")).when(testMessageConsumer).accept(Mockito.any(TestMessage.class));

        //Publish a normal event, listener is executed and an error is generated
        TestMessage testMessage = TestMessage.newBuilder()
                .setMessage("test")
                .build();

        sendTestMessageSync(TestMessageConsumer.TOPIC_NAME, testMessage);
        MessageProcessingFailedEvent resultingError = expectError();
        Mockito.verify(testMessageConsumer, Mockito.timeout(TEST_TIMEOUT)).accept(Mockito.any(TestMessage.class));

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

    private void sendTestMessageSync(String topic, SpecificRecord message) {
        try {
            ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>(topic, message);
            producerRecord.headers().add(KafkaTestConstants.TEST_PRODUCER_DISABLE_CONTRACT_CHECK_HEADER);
            CompletableFuture<SendResult<Object, Object>> future = kafkaTemplate.send(producerRecord);
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error sending interrupted");
        } catch (Exception e) {
            Assertions.fail("Could not send message", e);
        }
    }

    @Test
    void specificExceptionWhileEventHandling() {
        //Event handler will generate a specific exception
        SpecificException exception = SpecificException.builder()
                .description("desc")
                .errorCode("code")
                .message("msg")
                .temporality(MessageHandlerExceptionInformation.Temporality.TEMPORARY)
                .stackTraceAsString("stack")
                .build();
        Mockito.doThrow(exception).when(testEventProcessor).receive(Mockito.any());

        //Publish a normal event, listener is executed and an error is generated
        AvroMessage message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").message("message").build();
        sendSync(TestEventConsumer.TOPIC_NAME, message);
        MessageProcessingFailedEvent resultingError = expectError();
        Mockito.verify(testEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());

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
    void testNonDeserializableEvent() {
        //Publish garbage, listener is NOT executed and an error is generated
        publishGarbageEvent();
        MessageProcessingFailedEvent resultingError = expectError();
        Mockito.verifyNoMoreInteractions(testEventProcessor);

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
    void testWrongEvent() {
        //Publish wrong event, listener is NOT executed and an error is generated
        AvroMessage message = JmeCreateDeclarationCommandBuilder.create().idempotenceId("idempotenceId").text("message").build();
        sendSync(TestEventConsumer.TOPIC_NAME, message);
        MessageProcessingFailedEvent resultingError = expectError();
        Mockito.verifyNoMoreInteractions(testEventProcessor);

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

    private MessageProcessingFailedEvent expectError() {
        ArgumentCaptor<MessageProcessingFailedEvent> captor = ArgumentCaptor.forClass(MessageProcessingFailedEvent.class);
        Mockito.verify(errorEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(captor.capture());
        return captor.getValue();
    }

    private void publishGarbageEvent() {
        Map<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers(KafkaProperties.DEFAULT_CLUSTER));
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, kafkaProperties.getSecurityProtocol(KafkaProperties.DEFAULT_CLUSTER).name);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://none");
        props.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, MockSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MockSerializer.class);
        DefaultKafkaProducerFactory<?, byte[]> producerFactory = new DefaultKafkaProducerFactory<>(props);
        KafkaTemplate<?, byte[]> template = new KafkaTemplate<>(producerFactory);
        template.send(TestEventConsumer.TOPIC_NAME, new byte[]{0, 1});
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
