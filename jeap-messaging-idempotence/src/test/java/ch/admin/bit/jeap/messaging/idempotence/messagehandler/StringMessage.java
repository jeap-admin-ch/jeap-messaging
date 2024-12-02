package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import ch.admin.bit.jeap.messaging.model.*;
import lombok.Value;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Value
public class StringMessage implements Message {

    private final String messagePayloadText;
    private final String messageIdempotenceId;
    private final String messageTypeName;
    private final String messageId;
    private final Instant created;

    public static StringMessage from(String text, String idempotenceId, String messageTypeName) {
        return new StringMessage(text, idempotenceId, messageTypeName, UUID.randomUUID().toString(), Instant.now());
    }

    @Override
    public String toString() {
        return messagePayloadText;
    }

    @Override
    public MessagePayload getPayload() {
        return new StringMessagePayload();
    }

    @Override
    public Optional<? extends MessagePayload> getOptionalPayload() {
        return Optional.of(getPayload());
    }

    @Override
    public MessageIdentity getIdentity() {
        return new StringMessageIdentity();
    }

    @Override
    public MessagePublisher getPublisher() {
        return null;
    }

    @Override
    public MessageType getType() {
        return new StringMessageType();
    }

    @Override
    public MessageReferences getReferences() {
        return null;
    }

    @Override
    public Optional<? extends MessageReferences> getOptionalReferences() {
        return Optional.ofNullable(getReferences());
    }

    @Override
    public Optional<String> getOptionalProcessId() {
        return Optional.empty();
    }

    private class StringMessageIdentity implements MessageIdentity {
        @Override
        public String getId() {
            return messageId;
        }
        @Override
        public String getIdempotenceId() {
            return messageIdempotenceId;
        }
        @Override
        public Instant getCreated() {
            return created;
        }
    }

    private class StringMessageType implements MessageType {
        @Override
        public String getName() {
            return messageTypeName;
        }
        @Override
        public String getVersion() {
            return "1.0.0";
        }
    }

    private class StringMessagePayload implements MessagePayload {
        public String getText() {
            return messagePayloadText;
        }
    }

}
