package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;


public interface DeferredMessageSender {

    void sendAsImmediate(DeferredMessage deferredMessage);

    void sendAsScheduled(DeferredMessage deferredMessage);

}
