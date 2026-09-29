package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;

import java.util.Optional;

/**
 * Resolves the temporality of exceptions that do not provide
 * {@link MessageHandlerExceptionInformation} themselves.
 * <p>
 * Applications can replace the default Spring bean to add or override the
 * jEAP defaults used by the error service sender. A replacement resolver should
 * delegate to {@link DefaultExceptionTemporalityResolver} to keep the defaults.
 */
@FunctionalInterface
public interface ExceptionTemporalityResolver {

    /**
     * @param exception the exception that caused message processing to fail
     * @return the resolved temporality, or an empty result if the exception is unknown
     */
    Optional<MessageHandlerExceptionInformation.Temporality> resolve(Throwable exception);
}
