package ch.admin.bit.jeap.messaging.kafka.log;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.model.MessageUser;
import com.fasterxml.jackson.core.JsonGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.argument.StructuredArgument;

import java.io.IOException;

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
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStringField("messageType", message.getType().getName());
        generator.writeStringField("messageVersion", message.getType().getVersion());
        generator.writeStringField("messageId", message.getIdentity().getId());
        generator.writeStringField("messageIdempotenceId", message.getIdentity().getIdempotenceId());
        generator.writeStringField("messageCreated", message.getIdentity().getCreatedZoned().toString());
        generator.writeStringField("messagePublisherSystem", message.getPublisher().getSystem());
        generator.writeStringField("messagePublisherService", message.getPublisher().getService());
        generator.writeStringField("messageUserId", message.getOptionalUser().map(MessageUser::getId).orElse(null));
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
        public void writeTo(JsonGenerator generator) throws IOException {
            generator.writeStringField("messageType", messageType);
        }

        @Override
        public String toString() {
            return messageType;
        }
    }
}
