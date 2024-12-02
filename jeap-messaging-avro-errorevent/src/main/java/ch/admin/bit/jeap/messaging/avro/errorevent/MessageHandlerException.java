package ch.admin.bit.jeap.messaging.avro.errorevent;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A runtime exception that communicates specific information about an event handler's outcome to the
 * error-handling facility.
 * <p>
 * This type of exception can override the default presentation of event handler exceptions as UNKNOWN_EXCEPTION in
 * the error-handling UI. It can signal that retrying the handling of this event might yield a success if it is
 * declared as having EventHandlerExceptionInformation.Temporality.TEMPORARY
 * <p>
 * Default implementation of EventHandlerExceptionInformation.
 */
@Getter
public class MessageHandlerException extends RuntimeException implements MessageHandlerExceptionInformation {
    private static final String EMPTY_MESSAGE = "";

    @NonNull
    private final String errorCode;

    private final String description;

    @NonNull
    private final Temporality temporality;

    /**
     * Constructs the underlying RuntimeException with a message and/or a cause.
     */
    @Builder
    protected MessageHandlerException(
            @NonNull final MessageHandlerExceptionInformation.Temporality temporality,
            @Nullable final Throwable cause,
            @Nullable final String message,
            @NonNull final String errorCode,
            @Nullable final String description) {
        super(getMessageOrEmpty(message, cause), cause);
        this.temporality = temporality;
        this.errorCode = errorCode;
        this.description = description;
    }

    private static String getMessageOrEmpty(String message, Throwable cause) {
        return message == null ? (cause == null ? EMPTY_MESSAGE : cause.toString()) : message;
    }

    @Override
    public String getStackTraceAsString() {
        final StringWriter sw = new StringWriter();
        this.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
