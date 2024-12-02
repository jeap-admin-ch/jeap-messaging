package ch.admin.bit.jeap.messaging.avro.errorevent;


import jakarta.annotation.Nullable;

/**
 * The information that needs to be returned by an exception from an event handler. This interface can e.g.
 * by implemented by exception classes.
 */
public interface MessageHandlerExceptionInformation {
    /**
     * Returns an error of this type of exception. This code will be sent to the error server.
     * The meanings of these have to be defined by the actual application
     */
    String getErrorCode();

    /**
     * Get a description of this type of exception, e.g. a text or a link. This description will be sent to the error
     * server and can be showed there. Can be null if no description is available
     */
    @Nullable
    String getDescription();

    /**
     * Get the type of this error, basically if the error is temporary or permanent. This information will be sent to
     * the error server that can then decide to try a resend.
     */
    Temporality getTemporality();

    /**
     * Get the actual error message of this exception instance, correspond to {@link Exception#getMessage()}
     * Will be sent to the error server and can be showed there.
     */
    String getMessage();

    /**
     * In case of an exception the stack trace of the exception.
     * Will be sent to the error server and can be showed there. Can be null if no stack trace is available.
     */
    @Nullable
    String getStackTraceAsString();

    /**
     * The type of error,  basically if the error is temporary or permanent
     */
    enum Temporality {UNKNOWN, PERMANENT, TEMPORARY}

    /**
     * Some default error codes used by the error handler itself
     */
    enum StandardErrorCodes {UNKNOWN_EXCEPTION, INVALID_EXCEPTION, DESERIALIZATION_FAILED, WRONG_EVENT_TYPE}
}
