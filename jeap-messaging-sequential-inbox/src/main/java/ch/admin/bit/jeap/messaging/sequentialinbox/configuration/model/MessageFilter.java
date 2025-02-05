package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

public interface MessageFilter<T> {
    boolean filter(T message);
}
