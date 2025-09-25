package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
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
        "spring.application.name=myapp",
        "jeap.messaging.authentication.subscriber.allowNonJeapMessages=true"}) // we allow non jeap messages
@DirtiesContext
class SignatureCheckNonJeapMessageAllowedIT extends KafkaIntegrationTestBase {

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
    void sendNonJeapMessage_doNotFailBecausePropertySet() {
        TestMessage testMessage = TestMessage.newBuilder()
                .setMessage("test")
                .build();

        sendTestMessageSync(TestMessageConsumer.TOPIC_NAME, testMessage);

        Mockito.verify(testMessageConsumer, Mockito.timeout(TEST_TIMEOUT)).accept(Mockito.any(TestMessage.class));
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

}
