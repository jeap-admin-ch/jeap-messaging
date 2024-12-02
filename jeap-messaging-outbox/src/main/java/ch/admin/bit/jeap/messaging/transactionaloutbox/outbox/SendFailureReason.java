package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

public enum SendFailureReason {

    INVALID_TOPIC("invalid topic", true),
    UNAUTHORIZED_ON_TOPIC("unauthorized on topic", true),
    MESSAGE_TOO_LARGE("message too large", true),
    GENERAL("general", false);

    public final String reason;
    public final boolean causedByMessage;

    SendFailureReason(String reason, boolean causedByMessage) {
        this.reason = reason;
        this.causedByMessage = causedByMessage;
    }

    public String toString() {
        return reason;
    }
}
