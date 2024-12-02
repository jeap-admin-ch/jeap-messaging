package ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.TestMessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;

abstract class KafkaMultiClusterIntegrationTestBase {
    protected static final int TEST_TIMEOUT = 10000;

    @Autowired
    protected KafkaAdmin kafkaAdmin;
    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @BeforeEach
    void waitForKafkaListener() {
        registry.getListenerContainers()
                .forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }

    protected void sendSync(KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate, String topic, AvroMessage message) {
        TestMessageSender.sendSync(kafkaTemplate, topic, message);
    }
}
