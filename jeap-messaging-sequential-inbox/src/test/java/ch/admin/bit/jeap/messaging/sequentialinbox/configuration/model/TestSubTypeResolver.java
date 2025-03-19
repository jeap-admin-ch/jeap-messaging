package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

public class TestSubTypeResolver implements SubTypeResolver<TestEvent, TestSubTypes> {

    @Override
    public TestSubTypes resolveSubType(TestEvent avroMessage) {
        return TestSubTypes.APPLES;
    }
}
