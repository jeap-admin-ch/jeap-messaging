package ch.admin.bit.jeap.messaging.kafka.signature.exceptions;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import jakarta.annotation.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MessageSignatureValidationException extends RuntimeException implements MessageHandlerExceptionInformation {

    private static final String ERROR_CODE = "MESSAGE_SIGNATURE_VALIDATION_ERROR";
    private static final Temporality DEFAULT_TEMPORALITY = Temporality.PERMANENT;

    private final Temporality temporality;

    private MessageSignatureValidationException(String message) {
        this(DEFAULT_TEMPORALITY, message);
    }

    private MessageSignatureValidationException(final Temporality temporality, String message) {
        super(message);
        this.temporality = temporality;
    }

    private MessageSignatureValidationException(final Temporality temporality, String message, Throwable cause) {
        super(message, cause);
        this.temporality = temporality;
    }

    public static MessageSignatureValidationException invalidSignatureKey() {
        String message = String.format("Key signature is invalid");
        return new MessageSignatureValidationException(message);
    }

    public static MessageSignatureValidationException invalidSignatureValue(String messageTypeName, String service) {
        String message = String.format("Message %s value signature is invalid, publisher: %s.", messageTypeName, service);
        return new MessageSignatureValidationException(message);
    }

    public static MessageSignatureValidationException strictModeSignatureHeadersMissing(String messageTypeName, String service) {
        String message = String.format("Received message %s from %s without signature certificate but strict mode is enabled. Rejecting message.", messageTypeName, service);
        return new MessageSignatureValidationException(message);
    }

    public static MessageSignatureValidationException certificateHeaderMissing(String messageTypeName, String service) {
        String message = String.format("Received message %s from %s with signature certificate header but without signature header. Rejecting message.", messageTypeName, service);
        return new MessageSignatureValidationException(message);
    }

    public static MessageSignatureValidationException signatureHeaderMissing(String messageTypeName, String service) {
        String message = String.format("Received message %s from %s with signature but without signature certificate. Rejecting message.", messageTypeName, service);
        return new MessageSignatureValidationException(message);
    }

    public static MessageSignatureValidationException publisherNotAllowed(String messageTypeName, String service) {
        String message = String.format("Received message %s from %s which is not an allowed publisher. Rejecting message.", messageTypeName, service);
        return new MessageSignatureValidationException(Temporality.TEMPORARY, message);
    }

    public static MessageSignatureValidationException headersMissing(boolean isKey) {
        String message = String.format("%s headers missing", isKey ? "Key" : "Value");
        return new MessageSignatureValidationException(message);
    }

    public static MessageSignatureValidationException notAllowedMessageType(Object messageObject) {
        String message = String.format("%s is not allowed", messageObject);
        return new MessageSignatureValidationException(Temporality.TEMPORARY, message);
    }

    public static MessageSignatureValidationException signatureValidationFailed(Throwable cause) {
        String message = "Signature validation failed";
        return new MessageSignatureValidationException(Temporality.PERMANENT, message, cause);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }


    @Override
    public Temporality getTemporality() {
        return temporality;
    }

    @Nullable
    @Override
    public String getStackTraceAsString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        this.printStackTrace(pw);
        return sw.toString();
    }
}
