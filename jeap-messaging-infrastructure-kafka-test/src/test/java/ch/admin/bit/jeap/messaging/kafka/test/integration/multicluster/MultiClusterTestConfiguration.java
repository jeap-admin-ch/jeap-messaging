package ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.Getter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@TestConfiguration
class MultiClusterTestConfiguration {

    @Bean
    TestConsumer testConsumer() {
        return new TestConsumer();
    }

    @Getter
    static class TestConsumer {
        private final List<JmeDeclarationCreatedEvent> defaultClusterEvents = new ArrayList<>();
        private final List<MessageProcessingFailedEvent> defaultClusterFailedEvents = new ArrayList<>();
        private final List<JmeCreateDeclarationCommand> awsClusterCommands = new ArrayList<>();
        private final List<MessageProcessingFailedEvent> awsClusterFailedEvents = new ArrayList<>();

        @TestKafkaListener(topics = JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, groupId = "test-default")
        public void onEvent(JmeDeclarationCreatedEvent event) {
            defaultClusterEvents.add(event);
            throwErrorForTestIfMessageContainsError(event.getPayload().getMessage());
        }

        @TestKafkaListener(topics = JmeCreateDeclarationCommand.TypeRef.DEFAULT_TOPIC, groupId = "test-aws",
                containerFactory = "awsKafkaListenerContainerFactory")
        public void onEventForAws(JmeCreateDeclarationCommand command) {
            awsClusterCommands.add(command);
            throwErrorForTestIfMessageContainsError(command.getPayload().getText());
        }

        @TestKafkaListener(topics = "errorTopic", groupId = "test-default")
        public void onMessageProcessingFailedEvent(MessageProcessingFailedEvent event) {
            defaultClusterFailedEvents.add(event);
        }

        @TestKafkaListener(topics = "errorTopic", groupId = "test-aws",
                containerFactory = "awsKafkaListenerContainerFactory")
        public void onMessageProcessingFailedEventForAws(MessageProcessingFailedEvent event) {
            awsClusterFailedEvents.add(event);
        }

        private static void throwErrorForTestIfMessageContainsError(String text) {
            if (text.startsWith("error")) {
                throw new RuntimeException("forced test error due to message " + text);
            }
        }
    }
}
