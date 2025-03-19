package ch.admin.bit.jeap.messaging.sequentialinbox.spring;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageHandlerProvider {

    private final Map<String, SequentialInboxMessageHandler> messageHandlers = new ConcurrentHashMap<>();

    public SequentialInboxMessageHandler getHandlerForJeapMessageType(String jeapMessageType) {
        return messageHandlers.get(jeapMessageType);
    }

    protected void addHandler(String jeapMessageType, SequentialInboxMessageHandler messageHandler) {
        messageHandlers.put(jeapMessageType, messageHandler);
    }
}
