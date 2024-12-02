package ch.admin.bit.jeap.messaging.api;

import ch.admin.bit.jeap.messaging.model.Message;

/**
 * Base interface for all message publishers. You can inject this interface
 * into your beans to publish messages.
 */
public interface MessagePublisher<MessageType extends Message> {
    /**
     * Send a message
     */
    void send(MessageType event);
}
