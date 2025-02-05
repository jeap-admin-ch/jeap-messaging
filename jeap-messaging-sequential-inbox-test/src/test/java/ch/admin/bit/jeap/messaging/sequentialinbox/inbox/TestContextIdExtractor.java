package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.ContextIdExtractor;
import ch.admin.bit.jeap.messaging.sequentialinbox.test.TestEvent;

public class TestContextIdExtractor implements ContextIdExtractor<TestEvent> {

    @Override
    public String extractContextId(TestEvent message) {
        return "";
    }
}
