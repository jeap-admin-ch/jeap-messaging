package ch.admin.bit.jeap.messaging.kafka.test.integration.common;

import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JmeDeclarationCreatedEventOtherTopicConsumer {

    public static final String TOPIC_NAME = "other-topic";

    private boolean hasConsumed = false;

    @KafkaListener(topics = TOPIC_NAME)
    public void consume(final JmeDeclarationCreatedEvent event, Acknowledgment ack) {
        ack.acknowledge();
        hasConsumed = true;
    }

    public boolean hasConsumed() {
        return hasConsumed;
    }

}
