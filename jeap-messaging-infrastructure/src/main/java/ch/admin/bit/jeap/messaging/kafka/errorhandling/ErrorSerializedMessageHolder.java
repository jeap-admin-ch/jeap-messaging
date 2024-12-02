package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.avro.SerializedMessageHolder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ErrorSerializedMessageHolder implements SerializedMessageHolder {
    @Getter
    private final byte[] serializedMessage;
    @Getter
    private final Throwable cause;
}
