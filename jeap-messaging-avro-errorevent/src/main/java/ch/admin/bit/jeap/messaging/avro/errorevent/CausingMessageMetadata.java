package ch.admin.bit.jeap.messaging.avro.errorevent;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.model.MessageIdentity;
import ch.admin.bit.jeap.messaging.model.MessagePublisher;
import ch.admin.bit.jeap.messaging.model.MessageType;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

record CausingMessageMetadata(
        String eventId,
        String idempotenceId,
        Instant created,
        String system,
        String service,
        String messageTypeName,
        String messageTypeVersion,
        Map<String, ByteBuffer> headers) {

    static CausingMessageMetadata from(Message message, Headers headers, String... preservedHeaderNames) {
        Map<String, ByteBuffer> headerMap = createHeaderMap(headers, preservedHeaderNames);
        MessageIdentity identity = message == null ? null : message.getIdentity();
        MessagePublisher publisher = message == null ? null : message.getPublisher();
        MessageType type = message == null ? null : message.getType();
        // All fields are optional to avoid errors when generating the failed message
        // A possible cause for the original message to produce an error might be an incomplete message

        if (identity == null && publisher == null && type == null && headerMap.isEmpty()) {
            return null;
        }

        return new CausingMessageMetadata(
                identity != null ? identity.getId() : null,
                identity != null ? identity.getIdempotenceId() : null,
                identity != null ? identity.getCreated() : null,
                publisher != null ? publisher.getSystem() : null,
                publisher != null ? publisher.getService() : null,
                type != null ? type.getName() : null,
                type != null ? type.getVersion() : null,
                headerMap);
    }

    private static Map<String, ByteBuffer> createHeaderMap(Headers headers, String[] preservedHeaderNames) {
        Map<String, ByteBuffer> headerMap = new HashMap<>();
        for (String headerName : preservedHeaderNames) {
            Header header = headers.lastHeader(headerName);
            if (header != null) {
                byte[] headerValue = header.value();
                headerMap.put(headerName, ByteBuffer.wrap(headerValue));
            }
        }
        return headerMap;
    }
}
