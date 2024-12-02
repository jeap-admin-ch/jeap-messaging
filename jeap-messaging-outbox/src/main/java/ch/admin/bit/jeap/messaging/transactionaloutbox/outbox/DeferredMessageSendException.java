package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import lombok.Getter;

@Getter
public class DeferredMessageSendException extends RuntimeException {

    private SendFailureReason reason;

    private DeferredMessageSendException(String message, Exception e, SendFailureReason reason) {
        super(message, e);
        this.reason = reason;
    }

    private DeferredMessageSendException(String message, SendFailureReason reason) {
        super(message);
        this.reason = reason;
    }

    public static DeferredMessageSendException invalidTopicException(DeferredMessage dm, Exception e) {
        final String message = String.format("Illegal topic '%s' on %s.", dm.getTopic(), DeferredMessageLogArgument.from(dm));
        return new DeferredMessageSendException(message, e, SendFailureReason.INVALID_TOPIC);
    }

    public static DeferredMessageSendException topicAuthorizationException(DeferredMessage dm, Exception e) {
        final String message = String.format("Unauthorized on topic '%s' for %s.", dm.getTopic(), DeferredMessageLogArgument.from(dm));
        return new DeferredMessageSendException(message, e, SendFailureReason.UNAUTHORIZED_ON_TOPIC);
    }

    public static DeferredMessageSendException messageTooLargeException(DeferredMessage dm, Exception e) {
        final String message = String.format("Message too large for topic '%s': %s.", dm.getTopic(), DeferredMessageLogArgument.from(dm));
        return new DeferredMessageSendException(message, e, SendFailureReason.MESSAGE_TOO_LARGE);
    }

    public static DeferredMessageSendException generalSendException(DeferredMessage dm, Exception e) {
        final String message = String.format("Sending message to topic '%s' failed for %s.", dm.getTopic(), DeferredMessageLogArgument.from(dm));
        return new DeferredMessageSendException(message, e, SendFailureReason.GENERAL);
    }

    public static DeferredMessageSendException unknownCluster(DeferredMessage dm, String clusterName) {
        final String message = String.format("Sending message to cluster '%s' failed because no cluster with this name is configured: %s",
                clusterName, DeferredMessageLogArgument.from(dm));
        return new DeferredMessageSendException(message, SendFailureReason.GENERAL);
    }
}
