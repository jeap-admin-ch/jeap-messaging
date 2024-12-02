package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

public interface AfterCommitMessageSender {

    void sendImmediatelyAfterTransactionCommit(DeferredMessage deferredMessage);

}
