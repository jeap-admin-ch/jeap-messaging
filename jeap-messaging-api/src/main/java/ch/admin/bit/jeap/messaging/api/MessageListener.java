package ch.admin.bit.jeap.messaging.api;

import ch.admin.bit.jeap.messaging.model.Message;

/**
 * Base interface for all listener. You can inherit from this
 * interface to get the events of this type
 *
 * @param <MessageType> The actual message type this interface wants
 */
@FunctionalInterface
public interface MessageListener<MessageType extends Message> {
    void receive(MessageType event);
}
