package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.TransactionTimedOutException;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

/**
 * Classifies framework exceptions for which retrying the original message is
 * generally safe and useful. The cause chain is inspected, as frameworks and
 * applications usually wrap the exception that reveals a temporary failure.
 */
public class DefaultExceptionTemporalityResolver implements ExceptionTemporalityResolver {

    private static final String[] TEMPORARY_EXCEPTION_CLASS_NAMES = {
            "org.hibernate.exception.JDBCConnectionException",
            "org.hibernate.exception.LockAcquisitionException",
            "org.hibernate.QueryTimeoutException",
            "org.springframework.web.client.ResourceAccessException"
    };

    // SQLState class 08 (connection exception). Spring translates it to the generic
    // DataAccessResourceFailureException, which is not classified as it also covers permanent failures.
    private static final String CONNECTION_EXCEPTION_SQL_STATE_CLASS = "08";

    @Override
    public Optional<MessageHandlerExceptionInformation.Temporality> resolve(Throwable exception) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = exception;
        while (current != null && visited.add(current)) {
            Optional<MessageHandlerExceptionInformation.Temporality> explicitTemporality = explicitTemporality(current);
            if (explicitTemporality.isPresent()) {
                return explicitTemporality;
            }
            if (isTemporary(current)) {
                return Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY);
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    private static Optional<MessageHandlerExceptionInformation.Temporality> explicitTemporality(Throwable exception) {
        if (exception instanceof MessageHandlerExceptionInformation info) {
            MessageHandlerExceptionInformation.Temporality temporality = info.getTemporality();
            if (temporality != null && temporality != MessageHandlerExceptionInformation.Temporality.UNKNOWN) {
                return Optional.of(temporality);
            }
        }
        return Optional.empty();
    }

    private static boolean isTemporary(Throwable exception) {
        return exception instanceof RetriableException
                || exception instanceof TransientDataAccessException
                || exception instanceof RecoverableDataAccessException
                || exception instanceof TransactionTimedOutException
                || exception instanceof SQLTransientException
                || exception instanceof SQLRecoverableException
                || isConnectionException(exception)
                || hasTemporaryExceptionClassName(exception);
    }

    private static boolean isConnectionException(Throwable exception) {
        return exception instanceof SQLException sqlException
                && sqlException.getSQLState() != null
                && sqlException.getSQLState().startsWith(CONNECTION_EXCEPTION_SQL_STATE_CLASS);
    }

    // Matched by name to avoid compile-time dependencies; the superclass chain is checked so subclasses match too
    private static boolean hasTemporaryExceptionClassName(Throwable exception) {
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
