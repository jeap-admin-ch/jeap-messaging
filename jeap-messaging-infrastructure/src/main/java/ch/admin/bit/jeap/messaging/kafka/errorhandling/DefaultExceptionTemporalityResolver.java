package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.TransactionTimedOutException;

import java.util.Optional;

/**
 * Classifies framework exceptions for which retrying the original message is
 * generally safe and useful.
 */
public class DefaultExceptionTemporalityResolver implements ExceptionTemporalityResolver {

    private static final String[] TEMPORARY_EXCEPTION_CLASS_NAMES = {
            "org.hibernate.exception.JDBCConnectionException",
            "org.hibernate.exception.LockAcquisitionException",
            "org.hibernate.QueryTimeoutException",
            "org.springframework.web.client.ResourceAccessException"
    };

    @Override
    public Optional<MessageHandlerExceptionInformation.Temporality> resolve(Throwable exception) {
        if (exception instanceof MessageHandlerExceptionInformation info) {
            MessageHandlerExceptionInformation.Temporality temporality = info.getTemporality();
            if (temporality != null && temporality != MessageHandlerExceptionInformation.Temporality.UNKNOWN) {
                return Optional.of(temporality);
            }
        }
        return exception instanceof RetriableException
                || exception instanceof TransientDataAccessException
                || exception instanceof RecoverableDataAccessException
                || exception instanceof TransactionTimedOutException
                || (exception != null && hasTemporaryExceptionClassName(exception))
                ? Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY)
                : Optional.empty();
    }

    // Matched by name to avoid compile-time dependencies; the superclass chain is checked so subclasses match too
    private boolean hasTemporaryExceptionClassName(Throwable exception) {
        for (Class<?> type = exception.getClass(); type != null; type = type.getSuperclass()) {
            String className = type.getName();
            for (String temporaryExceptionClassName : TEMPORARY_EXCEPTION_CLASS_NAMES) {
                if (temporaryExceptionClassName.equals(className)) {
                    return true;
                }
            }
        }
        return false;
    }
}
