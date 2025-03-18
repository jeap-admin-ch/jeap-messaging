package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.MessageHeader;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.core.log.LogAccessor;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.SerializationUtils;

import java.util.List;
import java.util.Optional;

import static org.apache.kafka.clients.consumer.ConsumerRecord.NO_TIMESTAMP;
import static org.apache.kafka.clients.consumer.ConsumerRecord.NULL_SIZE;

@UtilityClass
@Slf4j
class Deserializers {
    private static final LogAccessor LOGGER = new LogAccessor(LogFactory.getLog(Deserializers.class));

    static <T> T deserialize(Deserializer<T> deserializer, String topic, byte[] bytes, List<MessageHeader> messageHeaders, boolean isKey) {
        Headers headers = new RecordHeaders();

        messageHeaders.forEach(messageHeader -> {
                log.debug("Add saved header '{}' to buffered message", messageHeader.getHeaderName());
                headers.add(messageHeader.getHeaderName(), messageHeader.getHeaderValue());
        });

        T object = deserializer.deserialize(topic, headers, bytes);
        checkDeserException(isKey, headers);
        return object;
    }

    private static void checkDeserException(boolean isKey, Headers headers) {
        String headerName = isKey ?
                SerializationUtils.KEY_DESERIALIZER_EXCEPTION_HEADER :
                SerializationUtils.VALUE_DESERIALIZER_EXCEPTION_HEADER;
        if (headers.lastHeader(headerName) != null) {
            throwDeserExceptionIfFound(headers, headerName);
        }
    }

    private static void throwDeserExceptionIfFound(Headers headers, String headerName) {
        ConsumerRecord<?, ?> recordWithHeaders = createEmptyRecordWithHeaders(headers);
        DeserializationException exceptionFromHeader = SerializationUtils
                .getExceptionFromHeader(recordWithHeaders, headerName, LOGGER);
        if (exceptionFromHeader != null) {
            throw exceptionFromHeader;
        }
    }

    private static ConsumerRecord<Object, Object> createEmptyRecordWithHeaders(Headers headers) {
        return new ConsumerRecord<>("topic", 0, 0, NO_TIMESTAMP,
                TimestampType.NO_TIMESTAMP_TYPE, NULL_SIZE, NULL_SIZE, null, null,
                headers, Optional.empty());
    }
}
