package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

public interface ContextIdExtractor<T> {
    String extractContextId(T message);
}