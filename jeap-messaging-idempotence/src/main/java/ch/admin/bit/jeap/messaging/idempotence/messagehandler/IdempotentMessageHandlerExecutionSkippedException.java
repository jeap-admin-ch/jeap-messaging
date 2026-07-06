package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException;
import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingIdentity;

/**
 * Thrown when a message handler execution is skipped because the idempotent processing entry for the message could
 * not be created.
 * <p>
 * This exception carries {@link ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation}
 * with temporality {@link ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation.Temporality#TEMPORARY}
 * and a specific error code. The jEAP error handling service will therefore resend the message automatically instead
 * of creating a manual task:
 * <ul>
 *     <li>If the entry could not be created because a concurrent handling of the same message committed the entry,
 *     the resent message will be recognized as already processed and will be skipped silently.</li>
 *     <li>If the concurrent handling did not commit (e.g. rolled back after a deadlock), or the entry could not be
 *     created for another (transient) reason, the resent message will be processed.</li>
 * </ul>
 */
public class IdempotentMessageHandlerExecutionSkippedException extends MessageHandlerException {

    /**
     * Error code signalling that the idempotent processing entry could not be created because the same message was
     * probably being handled concurrently by another idempotent message handler at the time.
     */
    public static final String ERROR_CODE_CONCURRENT_HANDLING = "IDEMPOTENT_PROCESSING_CONCURRENT_HANDLING";

    /**
     * Error code signalling that the idempotent processing entry could not be created because of an unexpected error,
     * e.g. the database being temporarily unavailable.
     */
    public static final String ERROR_CODE_PROCESSING_FAILED = "IDEMPOTENT_PROCESSING_FAILED";

    private static final String DESCRIPTION_CONCURRENT_HANDLING = """
            The message handler execution was skipped because the same message was probably being handled concurrently \
            by another message handler or service instance at the time. If the concurrent handling completed \
            successfully, a resend of the message will be recognized as already processed and skipped. Otherwise a \
            resend will process the message. Normally no manual intervention is required.""";

    private static final String DESCRIPTION_PROCESSING_FAILED = """
            The message handler execution was skipped because the idempotent processing entry for the message could \
            not be created due to an unexpected error, e.g. the database being temporarily unavailable. A resend will \
            process the message once the idempotent processing entry can be created again.""";

    private IdempotentMessageHandlerExecutionSkippedException(String errorCode, String description, String message, Throwable cause) {
        super(Temporality.TEMPORARY, cause, message, errorCode, description);
    }

    public static IdempotentMessageHandlerExecutionSkippedException concurrentHandlingIdempotentProcessingException(IdempotentProcessingIdentity idempotentProcessingIdentity, Throwable t) {
        String msg = """
                     Skipping processing for the message context '%s' with idempotence id '%s' because an idempotent processing entry
                     could not be created. Probably the same message is already being handled concurrently by another idempotent message handler.
                     """.formatted(idempotentProcessingIdentity.getContext(), idempotentProcessingIdentity.getId());
        return new IdempotentMessageHandlerExecutionSkippedException(ERROR_CODE_CONCURRENT_HANDLING, DESCRIPTION_CONCURRENT_HANDLING, msg, t);
    }

    public static IdempotentMessageHandlerExecutionSkippedException generalIdempotentProcessingException(IdempotentProcessingIdentity idempotentProcessingIdentity, Throwable t) {
        String msg = """
                     Skipping processing for the message context '%s' with idempotence id '%s' because an unexpected exception occurred
                     while creating an idempotent processing entry.
                     """.formatted(idempotentProcessingIdentity.getContext(), idempotentProcessingIdentity.getId());
        return new IdempotentMessageHandlerExecutionSkippedException(ERROR_CODE_PROCESSING_FAILED, DESCRIPTION_PROCESSING_FAILED, msg, t);
    }
}
