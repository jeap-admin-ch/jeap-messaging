package ch.admin.bit.jeap.messaging.kafka.test.integration.common;

import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MessagingMessageConsumer {

    @Autowired(required = false)
    private final List<MessagingMessageListener> testEventProcessors;

    @TestKafkaListener(topics = JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, groupId = "messaging-test-group")
    public void consumeJmeDeclarationCreatedEvent(final Message<Object> message, Acknowledgment ack) {
        testEventProcessors.forEach(testEventProcessor -> testEventProcessor.receive(message));
        ack.acknowledge();
    }

    @TestKafkaListener(topics = JmeCreateDeclarationCommand.TypeRef.DEFAULT_TOPIC, groupId = "messaging-test-group")
    public void consumeJmeCreateDeclarationCommand(final Message<Object> message, Acknowledgment ack) {
        testEventProcessors.forEach(testEventProcessor -> testEventProcessor.receive(message));
        ack.acknowledge();
    }

    @TestKafkaListener(topics = JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer.OTHER_TOPIC_NAME, groupId = "messaging-test-group")
    public void consumeCustomDeserializerPropertiesConsumer(final Message<Object> message, Acknowledgment ack) {
        testEventProcessors.forEach(testEventProcessor -> testEventProcessor.receive(message));
        ack.acknowledge();
    }

}