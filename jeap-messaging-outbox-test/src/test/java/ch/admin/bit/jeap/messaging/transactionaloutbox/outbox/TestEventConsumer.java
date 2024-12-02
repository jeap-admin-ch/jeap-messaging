package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestEvent;
import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestMessageKey;
import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TestEventConsumer {

    public static final String TOPIC = "test-topic";

    @Autowired(required = false)
    private final List<TestEventListener> testEventListeners;

    @KafkaListener(topics = TOPIC)
    public void consume(@Payload TestEvent event,
                        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) TestMessageKey key,
                        @Header(name = "jeapClusterName", required = false) String clusterName,
                        Acknowledgment ack) {
        log.debug("Consuming event {} sent with key {} from cluster {} (msg: {}).",
                event.getType().getName(), key != null ? key.getSomeProperty() : "n/a",
                clusterName,
                event.getOptionalPayload().map(TestPayload::getMessage).orElse(""));
        testEventListeners.forEach(listener -> listener.receive(event, key));
        ack.acknowledge();
    }
}
