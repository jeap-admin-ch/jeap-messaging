package ch.admin.bit.jeap.messaging.kafka.reactive.test;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaTestConstants;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;

import java.time.Duration;

public class ReactiveTestMessageSender {

    /**
     * Sends a kafka message synchronously, without checking for a producer contract being available
     */
    public static void sendSync(ReactiveKafkaProducerTemplate<AvroMessageKey, AvroMessage> producerTemplate,
                                String topic, AvroMessage message) {
        sendSync(producerTemplate, topic, message, true);
    }

    /**
     * Sends a kafka message synchronously, with the jEAP contract interceptor checking for a producer contract being available
     */
    public static void sendSyncEnsuringProducerContract(ReactiveKafkaProducerTemplate<AvroMessageKey, AvroMessage> producerTemplate,
                                                        String topic, AvroMessage message) {
        sendSync(producerTemplate, topic, message, false);
    }

    private static void sendSync(ReactiveKafkaProducerTemplate<AvroMessageKey, AvroMessage> producerTemplate,
                                 String topic, AvroMessage message, boolean disableProducerContractCheckForRecord) {
        ProducerRecord<AvroMessageKey, AvroMessage> producerRecord = new ProducerRecord<>(topic, message);
        if (disableProducerContractCheckForRecord) {
            producerRecord.headers().add(KafkaTestConstants.TEST_PRODUCER_DISABLE_CONTRACT_CHECK_HEADER);
        }
        producerTemplate.send(producerRecord).block(Duration.ofSeconds(5));
    }

}
