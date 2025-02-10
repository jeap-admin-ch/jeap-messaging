package ch.admin.bit.jeap.messaging.kafka.signature.exceptions;

public class MessageSignatureException extends RuntimeException {

    private MessageSignatureException(String message, Throwable cause) {
        super(message, cause);
    }

    public static MessageSignatureException signatureCreationFailed(Exception exception) {
        String message = "Error while creating signature";
        return new MessageSignatureException(message, exception);
    }
}
