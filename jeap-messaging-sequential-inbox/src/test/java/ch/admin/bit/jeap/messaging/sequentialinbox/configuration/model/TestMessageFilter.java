package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

public class TestMessageFilter implements MessageFilter<TestEvent> {

    @Override
    public boolean filter(TestEvent message) {
        return false;
    }
}
