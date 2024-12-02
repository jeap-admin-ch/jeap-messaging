package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.model.Message;

public class TransactionalOutboxException extends RuntimeException {

    protected TransactionalOutboxException(String message) {
        super(message);
    }

    protected TransactionalOutboxException(String message, Throwable cause) {
        super(message, cause);
    }

    public static TransactionalOutboxException messageSerializationFailed(Message m, String topic, String reason) {
        return new TransactionalOutboxException(getErrorMessageMessageSerializationFailed(m, topic, reason));
    }

    public static TransactionalOutboxException messageSerializationFailed(Message m, String topic, Exception e) {
        return new TransactionalOutboxException(getErrorMessageMessageSerializationFailed(m, topic, e.getMessage()), e);
    }

    private static String getErrorMessageMessageSerializationFailed(Message m, String topic, String reason) {
        return String.format("Serialization of message with type '%s', id '%s', idempotence id '%s' for topic '%s' failed: %s",
                m.getType().getName(), m.getIdentity().getId(), m.getIdentity().getIdempotenceId(), topic, reason);
    }

    public static TransactionalOutboxException messageKeySerializationFailed(Object key, String topic, String reason) {
        return new TransactionalOutboxException(getErrorMessageMessageKeySerializationFailed(key, topic, reason));
    }

    public static TransactionalOutboxException messageKeySerializationFailed(Object key, String topic, Exception e) {
        return new TransactionalOutboxException(getErrorMessageMessageKeySerializationFailed(key, topic, e.getMessage()), e);
    }

    private static String getErrorMessageMessageKeySerializationFailed(Object key, String topic, String reason) {
        return String.format("Serialization of key '%s' for topic '%s' failed: %s",
                key, topic, reason);
    }

    public static TransactionalOutboxException contractValidationFailed(Message message, Exception e) {
        String errorMessage = String.format("Contract validation for message of type '%s' failed.", message.getType().getName());
        return new TransactionalOutboxException(errorMessage, e);
    }

    public static TransactionalOutboxException deferredMessageNotFoundInOutbox(long deferredMessageId) {
        String errorMessage = String.format("There is no deferred message stored in the outbox with id %s.", deferredMessageId);
        return new TransactionalOutboxException(errorMessage);
    }

}
