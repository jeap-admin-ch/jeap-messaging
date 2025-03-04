package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.avro.SerializedMessageHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.serializer.FailedDeserializationInfo;

import java.util.function.Function;

/**
 * This class is used by the {@link org.springframework.kafka.support.serializer.ErrorHandlingDeserializer}. When a
 * message cannot be de-serialized (e.g. because its not a valid Avro-Message, it is not conform to the schema or the
 * schema is missing, the de-serializer will use this class to wrap the original message into a
 * {@link SerializedMessageHolder} so that it can be used by the error handler and be sent to the error service
 */
@Slf4j
public class CreateSerializedMessageHolder implements Function<FailedDeserializationInfo, Object> {

    @Override
    public SerializedMessageHolder apply(FailedDeserializationInfo failedDeserializationInfo) {
        log.error("Deserialization failed", failedDeserializationInfo.getException());
        return new ErrorSerializedMessageHolder(
                failedDeserializationInfo.getData(),
                failedDeserializationInfo.getException());
    }

    public static boolean deserializationFailed(Object recordKeyOrValue) {
        return recordKeyOrValue instanceof ErrorSerializedMessageHolder;
    }

}
