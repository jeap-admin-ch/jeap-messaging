package ch.admin.bit.jeap.messaging.sequentialinbox.spring;

import java.lang.reflect.Method;
import java.util.List;

public class SequentialInboxException extends RuntimeException {

    private SequentialInboxException(String message) {
        super(message);
    }

    private SequentialInboxException(String message, Exception cause) {
        super(message, cause);
    }

    public static SequentialInboxException noMessageHandlerFound(String messageType) {
        return new SequentialInboxException("No message handler found for message type " + messageType + ". Please configure a handler with a method annotated with @SequentialInboxMessageListener for this message type.");
    }
    
    public static SequentialInboxException multipleMessageHandlersFound(String messageType, List<SequentialInboxMessageHandler> beans) {
        return new SequentialInboxException("Multiple message handlers annotated with @SequentialInboxMessageListener found for message type " + messageType + ": " + beans);
    }

    public static SequentialInboxException handlerMethodCallFailed(Method method, Exception cause) {
        return new SequentialInboxException("Unable to call method " + method, cause);
    }
}