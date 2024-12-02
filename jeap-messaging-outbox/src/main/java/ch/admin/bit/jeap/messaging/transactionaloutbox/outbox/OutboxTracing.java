package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

public interface OutboxTracing {

    OutboxTraceContext retrieveCurrentTraceContext();

    void updateCurrentTraceContext(OutboxTraceContext outboxTraceContext);

}
