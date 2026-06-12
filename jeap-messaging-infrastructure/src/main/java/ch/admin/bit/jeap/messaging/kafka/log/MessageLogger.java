package ch.admin.bit.jeap.messaging.kafka.log;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.model.MessageUser;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.argument.StructuredArgument;
import tools.jackson.core.JsonGenerator;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class MessageLogger implements StructuredArgument {
    private final Message message;

    static MessageLogger message(Object message) {
        if (message instanceof Message msg) {
            return new MessageLogger(msg);
        } else {
            return new SimpleMessageLogger(message.getClass().getSimpleName());
        }
    }

    @Override
    public void writeTo(JsonGenerator generator) {
        generator.writeStringProperty("messageType", message.getType().getName());
        generator.writeStringProperty("messageVersion", message.getType().getVersion());
        if (message.getType().getVariant() != null) {
            generator.writeStringProperty("messageVariant", message.getType().getVariant());
        }
        generator.writeStringProperty("messageId", message.getIdentity().getId());
        generator.writeStringProperty("messageIdempotenceId", message.getIdentity().getIdempotenceId());
        generator.writeStringProperty("messageCreated", message.getIdentity().getCreatedZoned().toString());
        generator.writeStringProperty("messagePublisherSystem", message.getPublisher().getSystem());
        generator.writeStringProperty("messagePublisherService", message.getPublisher().getService());
        generator.writeStringProperty("messageUserId", message.getOptionalUser().map(MessageUser::getId).orElse(null));
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", message.getType().getName(), message.getIdentity().getId());
    }

    private static class SimpleMessageLogger extends MessageLogger {
        private final String messageType;

        public SimpleMessageLogger(String messageType) {
            super(null);
            this.messageType = messageType;
        }

        @Override
        public void writeTo(JsonGenerator generator) {
            generator.writeStringProperty("messageType", messageType);
        }

        @Override
        public String toString() {
            return messageType;
        }
    }
}
