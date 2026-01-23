package ch.admin.bit.jeap.messaging.kafka.test;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;

/**
 * Base class for Kafka integration tests. Starts an embedded Kafka using @{@link EmbeddedKafka}
 * and provides some common beans for your tests like a template etc. Waits for all listener containers to be
 * assigned to a topic before starting the test to make sure not records are lost in the listener.
 */
@EmbeddedKafka(controlledShutdown = true, partitions = 1)
abstract public class KafkaIntegrationTestBase {
    protected static final int TEST_TIMEOUT = 10000;

    @Autowired
    protected KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;
    @Autowired
    @SuppressWarnings("unused")
    protected KafkaAdmin kafkaAdmin;
    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @BeforeEach
    void waitForKafkaListener() {
        registry.getListenerContainers()
                .forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }

    protected void sendSync(String topic, AvroMessage message) {
        TestMessageSender.sendSync(kafkaTemplate, topic, message);
    }

    protected void sendSync(String topic, AvroMessageKey messageKey, AvroMessage message) {
        TestMessageSender.sendSync(kafkaTemplate, topic, messageKey, message);
    }

    protected void sendSyncEnsuringProducerContract(String topic, AvroMessage message) {
        TestMessageSender.sendSyncEnsuringProducerContract(kafkaTemplate, topic, message);
    }
    protected void sendSyncWithHeaders(String topic, AvroMessage message, Header... headers) {
        TestMessageSender.sendSyncWithHeaders(kafkaTemplate, topic, null, message, headers);
    }

}
