package ch.admin.bit.jeap.domainevent.api;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.messaging.api.MessageListener;

/**
 * Deprecated interface to listen to events. Currently kept for compatibility reasons
 *
 * @param <EventType> The type of events to listen to
 * @deprecated Use {@link MessageListener} instead
 */
@FunctionalInterface
@SuppressWarnings("unused")
@Deprecated
public interface EventListener<EventType extends DomainEvent> extends MessageListener<EventType> {
}
