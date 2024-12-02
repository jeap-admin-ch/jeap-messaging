package ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.Getter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@TestConfiguration
class DifferentConsumerProducerClusterTestConfiguration {

    @Bean
    TestConsumer testConsumer() {
        return new TestConsumer();
    }

    @Getter
    static class TestConsumer {
        private final List<JmeDeclarationCreatedEvent> consumerClusterEvents = new ArrayList<>();
        private final List<JmeDeclarationCreatedEvent> producerClusterEvents = new ArrayList<>();
        private final List<MessageProcessingFailedEvent> consumerClusterFailedEvents = new ArrayList<>();
        private final List<MessageProcessingFailedEvent> producerClusterFailedEvents = new ArrayList<>();

        @TestKafkaListener(topics = JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, groupId = "test-consumer")
        public void onEventForConsumerCluster(JmeDeclarationCreatedEvent event) {
            consumerClusterEvents.add(event);
            throwErrorForTestIfMessageContainsError(event.getPayload().getMessage());
        }

        @TestKafkaListener(topics = JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, groupId = "test-producer",
                containerFactory = "producerKafkaListenerContainerFactory")
        public void onEventForProducerCluster(JmeDeclarationCreatedEvent event) {
            producerClusterEvents.add(event);
            throwErrorForTestIfMessageContainsError(event.getPayload().getMessage());
        }

        @TestKafkaListener(topics = "errorTopic", groupId = "test-consumer")
        public void onMessageProcessingFailedEventForConsumerCluster(MessageProcessingFailedEvent event) {
            consumerClusterFailedEvents.add(event);
        }

        @TestKafkaListener(topics = "errorTopic", groupId = "test-producer",
                containerFactory = "producerKafkaListenerContainerFactory")
        public void onMessageProcessingFailedEventForProducerCluster(MessageProcessingFailedEvent event) {
            producerClusterFailedEvents.add(event);
        }

        private static void throwErrorForTestIfMessageContainsError(String text) {
            if (text.startsWith("error")) {
                throw new RuntimeException("forced test error due to message " + text);
            }
        }
    }
}
