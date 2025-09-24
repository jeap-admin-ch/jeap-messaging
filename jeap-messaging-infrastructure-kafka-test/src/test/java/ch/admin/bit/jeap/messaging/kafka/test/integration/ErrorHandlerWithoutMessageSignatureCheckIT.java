package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.subscriber.DefaultSignatureAuthenticityService;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaTestConstants;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestMessageConsumer;
import ch.admin.bit.jeap.test.avro.message.TestMessage;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = TestConfig.class, properties = {
        "spring.application.name=ErrorHandlerIT"})
@DirtiesContext
class ErrorHandlerWithoutMessageSignatureCheckIT extends KafkaIntegrationTestBase {
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

    // We mock the signature service to prevent signature validation errors
    // since the service cannot handle other than ch.admin.bit.jeap.messaging.model.Message.
    @MockitoBean
    private DefaultSignatureAuthenticityService signatureAuthenticityService;


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

    private MessageProcessingFailedEvent expectError() {
        ArgumentCaptor<MessageProcessingFailedEvent> captor = ArgumentCaptor.forClass(MessageProcessingFailedEvent.class);
        Mockito.verify(errorEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(captor.capture());
        return captor.getValue();
    }
}
