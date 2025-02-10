package ch.admin.bit.jeap.messaging.kafka.test.integration.common;

import org.springframework.messaging.Message;

public interface MessagingMessageListener {
    void receive(Message<?> message);
}
