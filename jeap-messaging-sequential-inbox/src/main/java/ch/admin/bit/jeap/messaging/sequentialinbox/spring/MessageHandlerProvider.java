package ch.admin.bit.jeap.messaging.sequentialinbox.spring;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageHandlerProvider {

    private final Map<String, SequentialInboxMessageHandler> messageHandlers = new ConcurrentHashMap<>();

    public SequentialInboxMessageHandler getHandlerForMessageType(String messageType) {
        return messageHandlers.get(messageType);
    }

    protected void addHandler(String messageType, SequentialInboxMessageHandler messageHandler) {
        messageHandlers.put(messageType, messageHandler);
    }
}
