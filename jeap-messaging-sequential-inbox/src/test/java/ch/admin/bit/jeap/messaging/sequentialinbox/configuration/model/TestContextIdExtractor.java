package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

public class TestContextIdExtractor implements ContextIdExtractor<TestEvent> {

    @Override
    public String extractContextId(TestEvent message) {
        return "";
    }

}
