package ch.admin.bit.jeap.messaging.kafka.signature.exceptions;

public class MessageSignatureConfigurationException extends RuntimeException {

    private MessageSignatureConfigurationException(String message) {
        super(message);
    }

    public static MessageSignatureConfigurationException signatureConfigurationFailure(String message) {
        return new MessageSignatureConfigurationException(message);
    }
}
