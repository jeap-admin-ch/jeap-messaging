package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxMessageListener;
import ch.admin.bit.jeap.messaging.sequentialinbox.test.TestEvent2;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j

@Getter
@Component
public class TestEvent2Listener {

    private final List<TestEvent2> events = new ArrayList<>();

    @SequentialInboxMessageListener
    public void handleMessage(TestEvent2 message, Acknowledgment acknowledgment) {
        log.info("Message received {} on topic", message.getIdentity().getCreated());
        events.add(message);
    }
}
