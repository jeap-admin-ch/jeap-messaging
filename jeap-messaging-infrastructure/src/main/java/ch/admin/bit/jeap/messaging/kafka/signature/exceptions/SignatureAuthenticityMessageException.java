package ch.admin.bit.jeap.messaging.kafka.signature.exceptions;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerMessageExceptionInformation;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import ch.admin.bit.jeap.messaging.model.Message;
import jakarta.annotation.Nullable;

public class SignatureAuthenticityMessageException extends RuntimeException implements MessageHandlerMessageExceptionInformation {

    private final transient Message message;
    private final transient MessageHandlerExceptionInformation messageHandlerExceptionInformation;

    private SignatureAuthenticityMessageException(Message message, MessageHandlerExceptionInformation messageHandlerExceptionInformation, Exception cause) {
        super(cause);
        this.message = message;
        this.messageHandlerExceptionInformation = messageHandlerExceptionInformation;
    }

    public static SignatureAuthenticityMessageException fromMessageHandlerExceptionInformation(Message message, MessageHandlerExceptionInformation messageHandlerExceptionInformation, Exception cause) {
        return new SignatureAuthenticityMessageException(message, messageHandlerExceptionInformation, cause);
    }

    @Override
    public Message getMessagingMessage() {
        return message;
    }

    @Override
    public String getErrorCode() {
        return messageHandlerExceptionInformation.getErrorCode();
    }

    @Nullable
    @Override
    public String getDescription() {
        return messageHandlerExceptionInformation.getDescription();
    }

    @Override
    public Temporality getTemporality() {
        return messageHandlerExceptionInformation.getTemporality();
    }

    @Nullable
    @Override
    public String getStackTraceAsString() {
        return messageHandlerExceptionInformation.getStackTraceAsString();
    }
}
