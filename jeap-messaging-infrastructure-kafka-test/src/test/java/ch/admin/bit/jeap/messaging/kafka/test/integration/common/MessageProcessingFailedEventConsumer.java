package ch.admin.bit.jeap.messaging.kafka.test.integration.common;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MessageProcessingFailedEventConsumer {
    @Autowired(required = false)
    private final List<MessageListener<MessageProcessingFailedEvent>> errorEventProcessors;

    @TestKafkaListener(topics = "errorTopic")
    public void consume(final MessageProcessingFailedEvent event, Acknowledgment ack) {
        errorEventProcessors.forEach(errorEventProcessor -> errorEventProcessor.receive(event));
        ack.acknowledge();
    }
}
