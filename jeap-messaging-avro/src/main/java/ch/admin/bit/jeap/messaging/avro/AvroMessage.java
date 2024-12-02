package ch.admin.bit.jeap.messaging.avro;

import ch.admin.bit.jeap.kafka.SerializedMessageReceiver;
import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.model.MessagePayload;
import ch.admin.bit.jeap.messaging.model.MessageReferences;
import org.apache.avro.generic.GenericContainer;

import java.util.Optional;

public interface AvroMessage extends Message, GenericContainer, SerializedMessageReceiver, SerializedMessageHolder {
    default void setProcessId(String processId) {
        throw AvroMessageBuilderException.onlyImplementedAfter("processId", "1.1.0");
    }

    default Optional<String> getOptionalProcessId() {
        return Optional.empty();
    }

    default void setReferences(MessageReferences references) {
        throw AvroMessageBuilderException.eventHasNotReferences(this.getClass());
    }

    default MessageReferences getReferences() {
        throw AvroMessageBuilderException.eventHasNotReferences(this.getClass());
    }

    default Optional<? extends MessageReferences> getOptionalReferences() {
        return Optional.empty();
    }

    default MessagePayload getPayload() {
        throw AvroMessageBuilderException.eventHasNotPayload(this.getClass());
    }

    default void setPayload(MessagePayload payload) {
        throw AvroMessageBuilderException.eventHasNotPayload(this.getClass());
    }

    @Override
    default Optional<? extends MessagePayload> getOptionalPayload() {
        return Optional.empty();
    }
}

