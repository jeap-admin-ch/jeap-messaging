package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import com.fasterxml.jackson.core.JsonGenerator;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.argument.StructuredArgument;

import java.io.IOException;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DeferredMessageLogArgument implements StructuredArgument {

    @NonNull
    private final DeferredMessage deferredMessage;

    public static DeferredMessageLogArgument from(DeferredMessage deferredMessage) {
        return new DeferredMessageLogArgument(deferredMessage);
    }

    @Override
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeNumberField("deferredMessageId", deferredMessage.getId());
        generator.writeStringField("deferredMessageTopic", deferredMessage.getTopic());
        generator.writeStringField("deferredMessageCreated",deferredMessage.getCreated().toString());
        generator.writeBooleanField("deferredMessageSendImmediately",deferredMessage.isSendImmediately());
        generator.writeStringField("messageId", deferredMessage.getMessageId());
        generator.writeStringField("messageIdempotenceId", deferredMessage.getMessageIdempotenceId());
        generator.writeStringField("messageType", deferredMessage.getMessageTypeName());
    }

    @Override
    public String toString() {
        return String.format("deferred message (deferredMessageId=%s)", deferredMessage.getId());
    }
}
