package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.TransactionTimedOutException;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

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

    // SQLState 08S02 (communication link changed): the AWS Advanced JDBC Wrapper reports it after a successful
    // failover, when the connection has been re-established but the interrupted operation was not replayed.
    private static final String CONNECTION_CHANGED_SQL_STATE = "08S02";

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
                || isCausedByConnectionChange(exception)
                ? Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY)
                : Optional.empty();
    }

    // Spring and Hibernate translate the SQLException, and applications usually wrap it, so the whole cause chain is
    // searched. An exception that declares its own temporality ends the search, as it decides about its causes.
    private static boolean isCausedByConnectionChange(Throwable exception) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = exception;
        while (current != null && visited.add(current)) {
            if (current != exception && hasExplicitTemporality(current)) {
                return false;
            }
            if (current instanceof SQLException sqlException
                    && CONNECTION_CHANGED_SQL_STATE.equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean hasExplicitTemporality(Throwable exception) {
        return exception instanceof MessageHandlerExceptionInformation info
                && info.getTemporality() != null
                && info.getTemporality() != MessageHandlerExceptionInformation.Temporality.UNKNOWN;
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
