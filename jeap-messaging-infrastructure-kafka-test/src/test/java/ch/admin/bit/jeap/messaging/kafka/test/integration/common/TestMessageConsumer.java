package ch.admin.bit.jeap.messaging.kafka.test.integration.common;

import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import ch.admin.bit.jeap.test.avro.message.TestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestMessageConsumer {

    public static final String TOPIC_NAME = "test-message-topic";

    @Autowired(required = false)
    private final List<Consumer<TestMessage>> testMessageListeners;

    @TestKafkaListener(topics = TOPIC_NAME)
    public void consume(TestMessage message, Acknowledgment ack) {
        testMessageListeners.forEach(l -> l.accept(message));
        ack.acknowledge();
    }
}
