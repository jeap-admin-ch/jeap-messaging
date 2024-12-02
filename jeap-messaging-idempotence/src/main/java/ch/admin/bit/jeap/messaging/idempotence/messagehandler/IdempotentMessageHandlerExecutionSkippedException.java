package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingIdentity;

public class IdempotentMessageHandlerExecutionSkippedException extends RuntimeException {

    public IdempotentMessageHandlerExecutionSkippedException(String message, Throwable t) {
        super(message, t);
    }

    public static IdempotentMessageHandlerExecutionSkippedException concurrentHandlingIdempotentProcessingException(IdempotentProcessingIdentity idempotentProcessingIdentity, Throwable t) {
        String msg = """
                     Skipping processing for the message context '%s' with idempotence id '%s' because an idempotent processing entry
                     could not be created. Probably the same message is already being handled concurrently by another idempotent message handler.
                     """.formatted(idempotentProcessingIdentity.getContext(), idempotentProcessingIdentity.getId());
        return new IdempotentMessageHandlerExecutionSkippedException(msg, t);
    }

    public static IdempotentMessageHandlerExecutionSkippedException generalIdempotentProcessingException(IdempotentProcessingIdentity idempotentProcessingIdentity, Throwable t) {
        String msg = """
                     Skipping processing for the message context '%s' with idempotence id '%s' because an unexpected exception occurred
                     while creating an idempotent processing entry.
                     """.formatted(idempotentProcessingIdentity.getContext(), idempotentProcessingIdentity.getId());
        return new IdempotentMessageHandlerExecutionSkippedException(msg, t);
    }
}
