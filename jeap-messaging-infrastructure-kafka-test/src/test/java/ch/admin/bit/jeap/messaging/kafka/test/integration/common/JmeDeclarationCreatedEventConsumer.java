package ch.admin.bit.jeap.messaging.kafka.test.integration.common;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class JmeDeclarationCreatedEventConsumer {

    public static final String TOPIC_NAME = JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC;

    @Autowired(required = false)
    private final List<MessageListener<JmeDeclarationCreatedEvent>> testEventProcessors;

    @TestKafkaListener(topics = TOPIC_NAME)
    public void consume(final JmeDeclarationCreatedEvent event, Acknowledgment ack) {
        testEventProcessors.forEach(testEventProcessor -> testEventProcessor.receive(event));
        ack.acknowledge();
    }
}
