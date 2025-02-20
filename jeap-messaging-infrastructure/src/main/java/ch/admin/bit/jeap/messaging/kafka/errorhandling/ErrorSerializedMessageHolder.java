package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.avro.SerializedMessageHolder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ErrorSerializedMessageHolder implements SerializedMessageHolder {
    private final byte[] serializedMessage;
    private final Throwable cause;
}
