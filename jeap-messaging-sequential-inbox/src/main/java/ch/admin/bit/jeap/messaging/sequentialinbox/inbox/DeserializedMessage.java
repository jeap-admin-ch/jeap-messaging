package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.CreateSerializedMessageHolder;

record DeserializedMessage(AvroMessageKey key, AvroMessage message, String topicName) {
    boolean deserializationFailed() {
        return CreateSerializedMessageHolder.deserializationFailed(key) || CreateSerializedMessageHolder.deserializationFailed(message);
    }
}
