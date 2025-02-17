package ch.admin.bit.jeap.messaging.kafka.test;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.Assertions;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

public class TestMessageSender {

    /**
     * Sends a kafka message synchronously, without checking for a producer contract being available
     */
    public static void sendSync(KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate,
                                String topic, AvroMessage message) {
        sendSync(kafkaTemplate, topic, message, true);
    }

    /**
     * Sends a kafka message synchronously, without checking for a producer contract being available
     */
    public static void sendSync(KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate,
                                String topic, AvroMessageKey messageKey, AvroMessage message) {
        sendSync(kafkaTemplate, topic, messageKey, message, true);
    }

    /**
     * Sends a kafka message synchronously, with the jEAP contract interceptor checking for a producer contract being available
     */
    public static void sendSyncEnsuringProducerContract(KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate,
                                                        String topic, AvroMessage message) {
        sendSync(kafkaTemplate, topic, message, false);
    }

    private static void sendSync(KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate,
                                 String topic, AvroMessage message, boolean disableProducerContractCheckForRecord) {
        sendSync(kafkaTemplate, topic, null, message, disableProducerContractCheckForRecord);
    }

    private static void sendSync(KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate,
                                 String topic, AvroMessageKey messageKey, AvroMessage message, boolean disableProducerContractCheckForRecord) {
            if (disableProducerContractCheckForRecord) {
                sendSyncWithHeaders(kafkaTemplate, topic, messageKey, message, KafkaTestConstants.TEST_PRODUCER_DISABLE_CONTRACT_CHECK_HEADER);
            }
            else {
                sendSyncWithHeaders(kafkaTemplate, topic, messageKey, message);
            }

    }

    public static void sendSyncWithHeaders(KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate,
                                String topic, AvroMessageKey messageKey, AvroMessage message, Header... headers) {
        try {
            ProducerRecord<AvroMessageKey, AvroMessage> producerRecord = new ProducerRecord<>(topic, messageKey, message);
            for (Header header : headers) {
                producerRecord.headers().add(header);
            }
            CompletableFuture<SendResult<AvroMessageKey, AvroMessage>> future = kafkaTemplate.send(producerRecord);
            future.get();
        } catch (InterruptedException e) {
            //In case of an interrupt finish current thread ASAP
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error sending interrupted");
        } catch (Exception e) {
            Assertions.fail("Could not send message", e);
        }
    }
}
