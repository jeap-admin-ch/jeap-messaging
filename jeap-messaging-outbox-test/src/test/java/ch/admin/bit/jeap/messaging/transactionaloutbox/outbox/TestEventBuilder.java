package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestEvent;
import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestPayload;
import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestReferences;
import lombok.Getter;

import java.util.UUID;

@Getter
public class TestEventBuilder extends AvroDomainEventBuilder<TestEventBuilder, TestEvent> {
    private final String specifiedMessageTypeVersion = "1.0.0";
    private final String serviceName = "test";
    private final String systemName = "test";
    private String message;

    private TestEventBuilder() {
        super(TestEvent::new);
    }

    public static TestEventBuilder create() {
        return new TestEventBuilder();
    }

    public static TestEventBuilder createWithRandomIdempotenceId() {
        return new TestEventBuilder().idempotenceId(UUID.randomUUID().toString());
    }

    public TestEventBuilder message(String message) {
        this.message = message;
        return self();
    }

    @Override
    protected TestEventBuilder self() {
        return this;
    }

    @Override
    public TestEvent build() {
        setReferences(TestReferences.newBuilder().build());
        setPayload(TestPayload.newBuilder().setMessage(message).build());
        return super.build();
    }
}
