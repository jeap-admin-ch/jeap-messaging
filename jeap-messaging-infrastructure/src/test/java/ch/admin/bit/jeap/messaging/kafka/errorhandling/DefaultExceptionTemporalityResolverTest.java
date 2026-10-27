package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateValidationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessResourceException;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultExceptionTemporalityResolverTest {

    private final DefaultExceptionTemporalityResolver resolver = new DefaultExceptionTemporalityResolver();

    @Test
    void resolvesRetryableKafkaExceptionAsTemporary() {
        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY),
                resolver.resolve(new TimeoutException()));
    }

    @Test
    void resolvesTransientSpringDataAccessExceptionAsTemporary() {
        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY),
                resolver.resolve(new TransientDataAccessResourceException("temporary")));
    }

    @Test
    void leavesUnknownExceptionUnclassified() {
        assertTrue(resolver.resolve(new IllegalStateException()).isEmpty());
    }

    @Test
    void classifiesWrappedTemporaryException() {
        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY),
                resolver.resolve(new RuntimeException(new TimeoutException())));
    }

    @Test
    void appliesExplicitTemporalityOfWrappedException() {
        CertificateValidationException permanentCause = CertificateValidationException.certificateExpired(null);
        RuntimeException wrapper = new RuntimeException("wrapped", permanentCause);

        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.PERMANENT),
                resolver.resolve(wrapper));
    }

    @Test
    void preservesExplicitPermanentTemporalityOfCurrentException() {
        CertificateValidationException permanent = CertificateValidationException.certificateExpired(null);
        permanent.initCause(new TimeoutException("timeout"));

        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.PERMANENT),
                resolver.resolve(permanent));
    }

    @Test
    void resolvesCauseOfExceptionWithUnknownTemporality() {
        MessageHandlerException unknown = MessageHandlerException.builder()
                .temporality(MessageHandlerExceptionInformation.Temporality.UNKNOWN)
                .errorCode("code")
                .cause(new TimeoutException("timeout"))
                .build();

        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY),
                resolver.resolve(unknown));
    }

    @ParameterizedTest
    @ValueSource(strings = {"08S02", "08006", "08001", "08007"})
    void resolvesTranslatedConnectionExceptionAsTemporary(String sqlState) {
        SQLException connectionFailure = new SQLException("The active SQL connection has changed", sqlState);
        RuntimeException exception = new RuntimeException("Failed updating process",
                new DataAccessResourceFailureException("could not execute batch", connectionFailure));

        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY),
                resolver.resolve(exception));
    }

    @Test
    void resolvesTransientAndRecoverableSqlExceptionsAsTemporary() {
        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY),
                resolver.resolve(new SQLTransientConnectionException("connection unavailable")));
        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY),
                resolver.resolve(new SQLRecoverableException("connection lost")));
    }

    @Test
    void leavesNonConnectionResourceFailureUnclassified() {
        assertTrue(resolver.resolve(new DataAccessResourceFailureException("no cause")).isEmpty());
        assertTrue(resolver.resolve(new DataAccessResourceFailureException("statement too complex",
                new SQLException("statement too complex", "54001"))).isEmpty());
        assertTrue(resolver.resolve(new SQLException("no SQLState")).isEmpty());
    }

    @Test
    void terminatesOnCyclicCauseChain() {
        RuntimeException wrapper = new RuntimeException("wrapper");
        IllegalStateException cause = new IllegalStateException("cause");
        wrapper.initCause(cause);
        cause.initCause(wrapper);

        assertTrue(resolver.resolve(wrapper).isEmpty());
    }
}
