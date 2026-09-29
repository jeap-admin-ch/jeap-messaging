package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateValidationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.TransientDataAccessResourceException;

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
}
