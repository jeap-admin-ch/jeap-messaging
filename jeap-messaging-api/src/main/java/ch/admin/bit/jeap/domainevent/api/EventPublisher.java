package ch.admin.bit.jeap.domainevent.api;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.messaging.api.MessagePublisher;

/**
 * Deprecated interface to publish events. Currently kept for compatibility reasons
 *
 * @param <EventType> The type of events to listen to
 * @deprecated Use {@link MessagePublisher} instead
 */
@FunctionalInterface
@SuppressWarnings("unused")
@Deprecated
public interface EventPublisher<EventType extends DomainEvent> extends MessagePublisher<EventType> {
}
