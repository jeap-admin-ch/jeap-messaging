package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxMessageListener;
import ch.admin.bit.jeap.messaging.sequentialinbox.test.TestEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Component
public class TestEventListener {

    private final List<TestEvent> events = new ArrayList<>();

    @SequentialInboxMessageListener
    public void handleMessage(TestEvent message, Acknowledgment acknowledgment) {
        log.info("Message received {} on topic", message.getIdentity().getCreated());
        events.add(message);
    }
}
