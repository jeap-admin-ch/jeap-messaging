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
    void doesNotClassifyWrappedTemporaryException() {
        assertTrue(resolver.resolve(new RuntimeException(new TimeoutException())).isEmpty());
    }

    @Test
    void doesNotClassifyWrappedExceptionWithExplicitTemporality() {
        CertificateValidationException permanentCause = CertificateValidationException.certificateExpired(null);
        RuntimeException wrapper = new RuntimeException("wrapped", permanentCause);

        assertTrue(resolver.resolve(wrapper).isEmpty());
    }

    @Test
    void preservesExplicitPermanentTemporalityOfCurrentException() {
        CertificateValidationException permanent = CertificateValidationException.certificateExpired(null);
        permanent.initCause(new TimeoutException("timeout"));

        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.PERMANENT),
                resolver.resolve(permanent));
    }

    @Test
    void doesNotTraverseCyclicCauseChain() {
        RuntimeException wrapper = new RuntimeException("wrapper");
        TimeoutException retryable = new TimeoutException("retryable");
        wrapper.initCause(retryable);
        retryable.initCause(wrapper);

        assertTrue(resolver.resolve(wrapper).isEmpty());
        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY),
                resolver.resolve(retryable));
    }

    @Test
    void resolvesWrappedFailoverAsTemporary() {
        SQLException failover = new SQLException("The active SQL connection has changed", "08S02");
        RuntimeException exception = new RuntimeException("Failed updating process",
                new DataAccessResourceFailureException("could not execute batch", failover));

        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY),
                resolver.resolve(exception));
    }

    @Test
    void resolvesFailoverCauseOfExceptionWithUnknownTemporalityAsTemporary() {
        MessageHandlerException unknown = MessageHandlerException.builder()
                .temporality(MessageHandlerExceptionInformation.Temporality.UNKNOWN)
                .errorCode("code")
                .cause(new DataAccessResourceFailureException("could not execute batch",
                        new SQLException("The active SQL connection has changed", "08S02")))
                .build();

        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY),
                resolver.resolve(unknown));
    }

    @ParameterizedTest
    @ValueSource(strings = {"08007", "08006", "08001", "54001"})
    void leavesOtherSqlStatesUnclassified(String sqlState) {
        DataAccessResourceFailureException exception = new DataAccessResourceFailureException("failed",
                new SQLException("failed", sqlState));

        assertTrue(resolver.resolve(exception).isEmpty());
    }

    @Test
    void preservesExplicitPermanentTemporalityOfExceptionCausedByFailover() {
        CertificateValidationException permanent = CertificateValidationException.certificateExpired(null);
        permanent.initCause(new SQLException("The active SQL connection has changed", "08S02"));

        assertEquals(Optional.of(MessageHandlerExceptionInformation.Temporality.PERMANENT),
                resolver.resolve(permanent));
    }

    @Test
    void doesNotClassifyFailoverBelowWrappedExceptionWithExplicitTemporality() {
        CertificateValidationException permanent = CertificateValidationException.certificateExpired(null);
        permanent.initCause(new SQLException("The active SQL connection has changed", "08S02"));

        assertTrue(resolver.resolve(new RuntimeException("wrapped", permanent)).isEmpty());
    }

    @Test
    void doesNotTraverseCyclicCauseChainWhenSearchingFailover() {
        RuntimeException wrapper = new RuntimeException("wrapper");
        IllegalStateException cause = new IllegalStateException("cause", wrapper);
        wrapper.initCause(cause);

        assertTrue(resolver.resolve(wrapper).isEmpty());
    }
}
