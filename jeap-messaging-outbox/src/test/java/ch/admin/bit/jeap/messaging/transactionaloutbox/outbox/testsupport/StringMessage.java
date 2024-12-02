package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport;

import ch.admin.bit.jeap.messaging.model.*;
import lombok.Value;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Value
public class StringMessage implements Message {

    private static final StringMessageType STRING_MESSAGE_TYPE = new StringMessageType();

    private final StringMessagePayload messagePayload;
    private final String messageId;
    private final String messageIdempotenceId;
    private final Instant created;


    public static StringMessage from(String str) {
        return new StringMessage(new StringMessagePayload(str), UUID.randomUUID().toString(), UUID.randomUUID().toString(), Instant.now());
    }

    @Override
    public String toString() {
        return Optional.ofNullable(messagePayload).map(StringMessagePayload::getMessage).orElse(null);
    }

    @Override
    public MessagePayload getPayload() {
        return messagePayload;
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
        return STRING_MESSAGE_TYPE;
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

    private static class StringMessageType implements MessageType {
        @Override
        public String getName() {
            return "StringMessageType";
        }
        @Override
        public String getVersion() {
            return "1.0.0";
        }
    }

}
