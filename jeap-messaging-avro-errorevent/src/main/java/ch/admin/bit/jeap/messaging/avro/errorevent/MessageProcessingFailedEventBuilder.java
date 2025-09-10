package ch.admin.bit.jeap.messaging.avro.errorevent;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jeap.messaging.avro.SerializedMessageHolder;
import ch.admin.bit.jeap.messaging.model.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@SuppressWarnings({"findbugs:SS_SHOULD_BE_STATIC"})
public class MessageProcessingFailedEventBuilder extends AvroDomainEventBuilder<MessageProcessingFailedEventBuilder, MessageProcessingFailedEvent> {

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap("EMPTY_CONTENT".getBytes());

    @Getter
    private final String eventName = "MessageProcessingFailedEvent";
    @Getter
    private final String specifiedMessageTypeVersion = "1.0.0";
    @Getter
    private String serviceName;
    @Getter
    private String systemName;

    private ConsumerRecord<?, ?> originalMessage;
    private MessageHandlerExceptionInformation messageHandlerExceptionInformation;
    private String processId = null;
    private CausingMessageMetadata causingMessageMetadata;
    private int stackTraceMaxLength;
    private String stackTraceHash;

    private MessageProcessingFailedEventBuilder() {
        super(MessageProcessingFailedEvent::new);
    }

    public static MessageProcessingFailedEventBuilder create() {
        return new MessageProcessingFailedEventBuilder();
    }

    public MessageProcessingFailedEventBuilder originalMessage(ConsumerRecord<?, ?> originalMessage, Message message, String... preservedHeaderNames) {
        this.originalMessage = originalMessage;
        this.processId = message == null ? null : message.getOptionalProcessId().orElse(null);
        this.causingMessageMetadata = CausingMessageMetadata.from(message, originalMessage.headers(), preservedHeaderNames);
        return self();
    }

    public MessageProcessingFailedEventBuilder eventHandleException(MessageHandlerExceptionInformation messageHandlerExceptionInformation) {
        this.messageHandlerExceptionInformation = messageHandlerExceptionInformation;
        return self();
    }

    public MessageProcessingFailedEventBuilder systemName(String systemName) {
        this.systemName = systemName;
        return self();
    }

    public MessageProcessingFailedEventBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return self();
    }

    public MessageProcessingFailedEventBuilder stackTraceMaxLength(int stackTraceMaxLength) {
        this.stackTraceMaxLength = stackTraceMaxLength;
        return self();
    }

    public MessageProcessingFailedEventBuilder stackTraceHash(String stackTraceHash) {
        this.stackTraceHash = stackTraceHash;
        return self();
    }

    @Override
    protected MessageProcessingFailedEventBuilder self() {
        return this;
    }

    @Override
    public MessageProcessingFailedEvent build() {
        if (this.originalMessage == null) {
            throw AvroMessageBuilderException.propertyNull("errorReferences.message");
        }
        if (this.messageHandlerExceptionInformation == null) {
            throw AvroMessageBuilderException.propertyNull("errorReferences.exception");
        }

        ErrorTypeReference errorType = ErrorTypeReference.newBuilder()
                .setCode(messageHandlerExceptionInformation.getErrorCode())
                .setTemporality(messageHandlerExceptionInformation.getTemporality().toString())
                .setType("exception")
                .build();
        MessageReference messageReference = MessageReference.newBuilder()
                .setOffset(String.valueOf(originalMessage.offset()))
                .setPartition(String.valueOf(originalMessage.partition()))
                .setTopicName(originalMessage.topic())
                .setType("message")
                .build();
        MessageProcessingFailedReferences errorReferences = MessageProcessingFailedReferences.newBuilder()
                .setMessage(messageReference)
                .setErrorType(errorType)
                .build();
        MessageProcessingFailedPayload errorPayload = MessageProcessingFailedPayload.newBuilder()
                .setErrorMessage(messageHandlerExceptionInformation.getMessage())
                .setOriginalKey(getSerializedMessage(originalMessage.key(), null))
                .setOriginalMessage(getSerializedMessage(originalMessage.value(), EMPTY_BUFFER))
                .setStackTrace(truncateStackTrace(messageHandlerExceptionInformation.getStackTraceAsString()))
                .setStackTraceHash(stackTraceHash)
                .setErrorDescription(messageHandlerExceptionInformation.getDescription())
                .setFailedMessageMetadata(buildFailedMessageMetadata())
                .build();
        setPayload(errorPayload);
        setReferences(errorReferences);
        setProcessId(this.processId);
        // Usually, UUIDs are bad idempotence IDs. However, each processing failure of an event is a unique occurrence
        // of the failure, UUIDs are thus valid in this case.
        idempotenceId(UUID.randomUUID().toString());
        return super.build();
    }

    private String truncateStackTrace(String stacktrace) {
        if (stacktrace == null || stacktrace.length() <= stackTraceMaxLength) {
            return stacktrace;
        }
        return stacktrace.substring(0, stackTraceMaxLength) + "...";
    }

    private FailedMessageMetadata buildFailedMessageMetadata() {
        if (causingMessageMetadata == null) {
            return null;
        }
        return FailedMessageMetadata.newBuilder()
                .setCreated(causingMessageMetadata.created())
                .setEventId(causingMessageMetadata.eventId())
                .setIdempotenceId(causingMessageMetadata.idempotenceId())
                .setService(causingMessageMetadata.service())
                .setSystem(causingMessageMetadata.system())
                .setMessageTypeName(causingMessageMetadata.messageTypeName())
                .setMessageTypeVersion(causingMessageMetadata.messageTypeVersion())
                .setHeaders(causingMessageMetadata.headers())
                .build();
    }

    private ByteBuffer getSerializedMessage(Object obj, ByteBuffer defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof SerializedMessageHolder smh) {
            return Optional.ofNullable(smh.getSerializedMessage()).map(ByteBuffer::wrap).orElse(defaultValue);
        }
        if (obj instanceof byte[] bytes) {
            return ByteBuffer.wrap(bytes);
        }
        log.error("Could not get serialized message from type " + obj.getClass());
        return defaultValue;
    }
}
