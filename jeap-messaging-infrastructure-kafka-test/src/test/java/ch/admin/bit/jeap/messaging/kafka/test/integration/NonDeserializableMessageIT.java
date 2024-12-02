package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.KafkaConfiguration;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
//Claim to be jme-messaging-subscriber-service so that we have a contract for JmeDeclarationEvent in Version 3. Also we want to be
//able to send events without contact to test that we do not get them
@SpringBootTest(classes = TestConfig.class, properties = {
        "spring.application.name=jme-messaging-subscriber-service"})
@Slf4j
@DirtiesContext
class NonDeserializableMessageIT extends KafkaIntegrationTestBase {
    //Register some event listener
    @MockBean
    MessageListener<MessageProcessingFailedEvent> messageProcessingFailedListener;
    @Autowired
    ProducerFactory<AvroMessageKey, AvroMessage> producerFactory;
    @Autowired
    KafkaConfiguration kafkaConfiguration;
    @Captor
    ArgumentCaptor<MessageProcessingFailedEvent> captor;

    @Test
    void consumeEventWithContract() throws Exception {
        Map<String, Object> props = kafkaConfiguration.producerConfig(KafkaProperties.DEFAULT_CLUSTER);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, "");
        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(props);
        KafkaTemplate<String, String> nonAvroKafkaTemplate = new KafkaTemplate<>(producerFactory);

        nonAvroKafkaTemplate.send(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, "this-is-not-avro")
                .get(10, TimeUnit.SECONDS);

        verify(messageProcessingFailedListener, timeout(Duration.ofSeconds(30).toMillis()))
                .receive(captor.capture());
        MessageProcessingFailedEvent failedEvent = captor.getValue();
        assertTrue(failedEvent.getPayload().getErrorMessage().contains("Could not deserialize value"));
        assertTrue(failedEvent.getPayload().getStackTrace().contains("Unknown magic byte"));
    }
}
