package ch.admin.bit.jeap.messaging.kafka.signature.exceptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageSignatureException extends RuntimeException {

    private MessageSignatureException(String message, Throwable cause) {
        super(message, cause);
    }

    public static MessageSignatureException signatureCreationFailed(Exception exception) {
        String message = "Error while creating signature";
        log.error(message, exception);
        return new MessageSignatureException(message, exception);
    }
}
