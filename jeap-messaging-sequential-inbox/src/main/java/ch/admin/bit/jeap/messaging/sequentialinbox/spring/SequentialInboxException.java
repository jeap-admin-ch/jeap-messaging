package ch.admin.bit.jeap.messaging.sequentialinbox.spring;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.Sequence;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessage;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class SequentialInboxException extends RuntimeException {

    private SequentialInboxException(String message) {
        super(message);
    }

    private SequentialInboxException(String message, Throwable cause) {
        super(message, cause);
    }

    public static SequentialInboxException noMessageHandlerFound(String messageType) {
        return new SequentialInboxException(("No message handler found for message type %s. Please configure a handler with a " +
                "method annotated with @SequentialInboxMessageListener for this message type.").formatted(messageType));
    }

    public static SequentialInboxException multipleMessageHandlersFound(String messageType, List<SequentialInboxMessageHandler> beans) {
        return new SequentialInboxException(("Multiple message handlers annotated with @SequentialInboxMessageListener found " +
                "for message type %s: %s").formatted(messageType, beans));
    }

    public static SequentialInboxException handlerMethodReflectiveOperationFailed(Method method, Exception cause) {
        return new SequentialInboxException("Reflection error while calling message handler method " + method, cause);
    }

    public static SequentialInboxException messageTypeNotConfiguredInAnySequence(String messageTypeName) {
        return new SequentialInboxException(("Message type %s is not configured in any message sequence configuration, " +
                "but has been received in the sequenced message listener.").formatted(messageTypeName));
    }

    public static SequentialInboxException failedToReadExistingSequenceInstance(Sequence sequence, String contextId, DataIntegrityViolationException e) {
        return new SequentialInboxException(
                "Failed to retrieve SequenceInstance of type %s with contextId %s after concurrent insertion"
                        .formatted(sequence.getName(), contextId), e);
    }

    public static SequentialInboxException invalidListenerMethodSignature(Method method) {
        return new SequentialInboxException("Listener method %s has an invalid signature. It must have one or two parameters: (? extends AvroMessage) or (? extends AvroMessageKey, ? extends AvroMessage)."
                .formatted(method));
    }

    public static SequentialInboxException unusedMessageHandlers(Set<ListenerBeanMethod> notStartedListeners) {
        return new SequentialInboxException(("The following %s annotated methods have not been started because there is " +
                "no corresponding message type declaration in the sequential inbox configuration: %s")
                .formatted(SequentialInboxMessageListener.class.getSimpleName(), notStartedListeners));
    }

    public static SequentialInboxException deserializationFailed(SequencedMessage sequencedMessage) {
        return new SequentialInboxException("Failed to deserialize record for sequenced message type %s in sequence %d"
                .formatted(sequencedMessage.getMessageType(), sequencedMessage.getSequenceInstanceId()));
    }

    public static SequentialInboxException gettingDefaultTopicFailed(Class<AvroMessage> messageTypeClass, Exception ex) {
        return new SequentialInboxException("Could not get default topic name for message type " + messageTypeClass, ex);
    }

    public static SequentialInboxException defaultTopicNotFound(Class<AvroMessage> messageTypeClass) {
        return new SequentialInboxException("No default topic found in message type %s".formatted(messageTypeClass.getName()));
    }

    public static SequentialInboxException typeRefNotFound(Class<AvroMessage> messageType) {
        return new SequentialInboxException("No TypeRef class found in message type %s, could not get default topic name"
                .formatted(messageType.getName()));
    }

    public static SequentialInboxException sequenceInstanceNotFound(Sequence sequence, String contextId) {
        return new SequentialInboxException("SequenceInstance of type %s with contextId %s not found"
                .formatted(sequence.getName(), contextId));
    }

    public static SequentialInboxException handlerMethodInvocationFailed(Method method, Throwable throwable) {
        return new SequentialInboxException("Invocation of message handler method %s failed".formatted(method), throwable);
    }
}
