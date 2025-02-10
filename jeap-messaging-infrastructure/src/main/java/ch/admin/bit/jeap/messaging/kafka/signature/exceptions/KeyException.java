package ch.admin.bit.jeap.messaging.kafka.signature.exceptions;

public class KeyException extends RuntimeException {

    private KeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public static KeyException couldNotCreateKeyException(Exception exception) {
        String message = "Could not create key";
        return new KeyException(message, exception);
    }
}
