package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.FailedDeserializationInfo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CreateSerializedMessageHolderTest {

    @Test
    void returnSerializedMessage() {
        byte[] data = new byte[]{1, 0, 1};
        Exception cause = new Exception();
        CreateSerializedMessageHolder target = new CreateSerializedMessageHolder();
        FailedDeserializationInfo failedDeserializationInfo = new FailedDeserializationInfo(null, null, data, false, cause);

        ErrorSerializedMessageHolder result = (ErrorSerializedMessageHolder) target.apply(failedDeserializationInfo);

        assertArrayEquals(data, result.getSerializedMessage());
        assertSame(cause, result.getCause());
    }
}
