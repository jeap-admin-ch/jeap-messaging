package ch.admin.bit.jeap.messaging.avro.errorevent;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventIdentity;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventPublisher;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventType;
import ch.admin.bit.jeap.domainevent.avro.event.integration.idl.IdlTestIntegrationEvent;
import ch.admin.bit.jeap.messaging.model.Message;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class MessageProcessingFailedEventBuilderTest {
    private final static ConsumerRecord<?, ?> originalMessage = new ConsumerRecord<>("Topic", 1, 2, new byte[]{0, 1}, new byte[]{1, 1});
    private final static ConsumerRecord<byte[], Message> originalJeapMessage =
            new ConsumerRecord<>("Topic", 1, 2, new byte[]{0, 1}, createTestMessage());
    private final static ConsumerRecord<byte[], Message> originalJeapMessageWithPreservedHeader =
            new ConsumerRecord<>("Topic", 1, 2L, 0L, TimestampType.CREATE_TIME, 0, 0, new byte[]{0, 1}, createTestMessage(), createHeaders(), Optional.empty());

    private static Headers createHeaders() {
        RecordHeaders headers = new RecordHeaders();
        headers.add("the-header", "header-value".getBytes(UTF_8));
        return headers;
    }

    private static Message createTestMessage() {
        IdlTestIntegrationEvent event = new IdlTestIntegrationEvent();
        event.setIdentity(AvroDomainEventIdentity.newBuilder()
                .setCreated(Instant.MIN)
                .setEventId("id")
                .setIdempotenceId("idempotence-id").build());
        event.setPublisher(AvroDomainEventPublisher.newBuilder()
                .setService("test-service")
                .setSystem("test-system")
                .build());
        event.setType(AvroDomainEventType.newBuilder()
                .setName("TestType")
                .setVersion("3.0.3")
                .build());
        event.setSerializedMessage(new byte[]{1, 1});
        return event;
    }

    private final static TestException eventHandleException = new TestException("Test", "stack", "100", MessageHandlerExceptionInformation.Temporality.UNKNOWN);

    @Test
    void permanentExceptionReference() {
        TestException eventHandleException = new TestException("Test", "stack", "100", MessageHandlerExceptionInformation.Temporality.PERMANENT);
        MessageProcessingFailedEvent messageProcessingFailedEvent = MessageProcessingFailedEventBuilder.create()
                .eventHandleException(eventHandleException)
                .serviceName("service")
                .systemName("system")
                .originalMessage(originalMessage, null)
                .build();

        assertEquals("100", messageProcessingFailedEvent.getReferences().getErrorType().getCode());
        assertEquals("PERMANENT", messageProcessingFailedEvent.getReferences().getErrorType().getTemporality());
        assertEquals("exception", messageProcessingFailedEvent.getReferences().getErrorType().getType());
        assertTrue(messageProcessingFailedEvent.getPayload().getOptionalFailedMessageMetadata().isEmpty());
    }

    @Test
    void temporaryExceptionReference() {
        TestException eventHandleException = new TestException("Test", "stack", "100", MessageHandlerExceptionInformation.Temporality.TEMPORARY);
        MessageProcessingFailedEvent messageProcessingFailedEvent = MessageProcessingFailedEventBuilder.create()
                .eventHandleException(eventHandleException)
                .serviceName("service")
                .systemName("system")
                .originalMessage(originalMessage, null)
                .build();

        assertEquals("100", messageProcessingFailedEvent.getReferences().getErrorType().getCode());
        assertEquals("TEMPORARY", messageProcessingFailedEvent.getReferences().getErrorType().getTemporality());
        assertEquals("exception", messageProcessingFailedEvent.getReferences().getErrorType().getType());
    }

    @Test
    void description() {
        TestException eventHandleException = new TestException("Test", "stack", "100", MessageHandlerExceptionInformation.Temporality.TEMPORARY, "desc");
        MessageProcessingFailedEvent messageProcessingFailedEvent = MessageProcessingFailedEventBuilder.create()
                .eventHandleException(eventHandleException)
                .serviceName("service")
                .systemName("system")
                .originalMessage(originalMessage, null)
                .build();

        assertTrue(messageProcessingFailedEvent.getOptionalPayload().isPresent());
        assertEquals("desc", messageProcessingFailedEvent.getOptionalPayload().get().getErrorDescription());
    }

    @Test
    void messageReference() {
        MessageProcessingFailedEvent messageProcessingFailedEvent = MessageProcessingFailedEventBuilder.create()
                .eventHandleException(eventHandleException)
                .serviceName("service")
                .systemName("system")
                .originalMessage(originalMessage, null)
                .build();

        assertEquals("2", messageProcessingFailedEvent.getReferences().getMessage().getOffset());
        assertEquals("1", messageProcessingFailedEvent.getReferences().getMessage().getPartition());
        assertEquals("Topic", messageProcessingFailedEvent.getReferences().getMessage().getTopicName());
        assertEquals("message", messageProcessingFailedEvent.getReferences().getMessage().getType());
    }

    @Test
    void publisher() {
        MessageProcessingFailedEvent messageProcessingFailedEvent = MessageProcessingFailedEventBuilder.create()
                .eventHandleException(eventHandleException)
                .serviceName("service")
                .systemName("system")
                .originalMessage(originalMessage, null)
                .build();

        assertEquals("system", messageProcessingFailedEvent.getPublisher().getSystem());
        assertEquals("service", messageProcessingFailedEvent.getPublisher().getService());
    }

    @Test
    void payload() {
        MessageProcessingFailedEvent messageProcessingFailedEvent = MessageProcessingFailedEventBuilder.create()
                .eventHandleException(eventHandleException)
                .serviceName("service")
                .systemName("system")
                .originalMessage(originalMessage, null)
                .stackTraceMaxLength(500)
                .build();

        assertTrue(messageProcessingFailedEvent.getOptionalPayload().isPresent());
        assertEquals("Test", messageProcessingFailedEvent.getOptionalPayload().get().getErrorMessage());
        assertEquals("stack", messageProcessingFailedEvent.getOptionalPayload().get().getStackTrace());
        assertArrayEquals(new byte[]{1, 1}, messageProcessingFailedEvent.getOptionalPayload().get().getOriginalMessage().array());
        assertArrayEquals(new byte[]{0, 1}, messageProcessingFailedEvent.getOptionalPayload().get().getOriginalKey().array());
        assertEquals("system", messageProcessingFailedEvent.getPublisher().getSystem());
        assertEquals("service", messageProcessingFailedEvent.getPublisher().getService());
    }

    @Test
    void payload_truncated_stackTrace() {
        MessageProcessingFailedEvent messageProcessingFailedEvent = MessageProcessingFailedEventBuilder.create()
                .eventHandleException(eventHandleException)
                .serviceName("service")
                .systemName("system")
                .originalMessage(originalMessage, null)
                .stackTraceMaxLength(3)
                .build();

        assertTrue(messageProcessingFailedEvent.getOptionalPayload().isPresent());
        assertEquals("sta...", messageProcessingFailedEvent.getOptionalPayload().get().getStackTrace());
    }

    @Test
    void payload_null_stackTrace() {
        MessageProcessingFailedEvent messageProcessingFailedEvent = MessageProcessingFailedEventBuilder.create()
                .eventHandleException(new TestException("Test", null, "100", MessageHandlerExceptionInformation.Temporality.UNKNOWN))
                .serviceName("service")
                .systemName("system")
                .originalMessage(originalMessage, null)
                .stackTraceMaxLength(3)
                .stackTraceHash(null)
                .build();

        assertTrue(messageProcessingFailedEvent.getOptionalPayload().isPresent());
        assertNull(messageProcessingFailedEvent.getOptionalPayload().get().getStackTrace());
        assertNull(messageProcessingFailedEvent.getOptionalPayload().get().getStackTraceHash());
    }

    @Test
    void stackTraceHash_NonNull() {
        MessageProcessingFailedEvent messageProcessingFailedEvent = MessageProcessingFailedEventBuilder.create()
                .eventHandleException(eventHandleException)
                .serviceName("service")
                .systemName("system")
                .originalMessage(originalMessage, null)
                .stackTraceHash("test-hash")
                .build();
        assertTrue(messageProcessingFailedEvent.getOptionalPayload().isPresent());
        assertEquals("test-hash", messageProcessingFailedEvent.getOptionalPayload().get().getStackTraceHash());
    }

    @Test
    void stackTraceHash_Null() {
        MessageProcessingFailedEvent messageProcessingFailedEvent = MessageProcessingFailedEventBuilder.create()
                .eventHandleException(eventHandleException)
                .serviceName("service")
                .systemName("system")
                .originalMessage(originalMessage, null)
                .stackTraceHash(null)
                .build();

        assertTrue(messageProcessingFailedEvent.getOptionalPayload().isPresent());
        assertNull(messageProcessingFailedEvent.getOptionalPayload().get().getStackTraceHash());
    }

    @Test
    void metadata() {
        byte[] headerValue = "header-value".getBytes(UTF_8);
        MessageProcessingFailedEvent messageProcessingFailedEvent = MessageProcessingFailedEventBuilder.create()
                .eventHandleException(eventHandleException)
                .serviceName("service")
                .systemName("system")
                .originalMessage(originalJeapMessageWithPreservedHeader, originalJeapMessageWithPreservedHeader.value(), "the-header")
                .build();

        assertTrue(messageProcessingFailedEvent.getPayload().getOptionalFailedMessageMetadata().isPresent());
        FailedMessageMetadata failedMessageMetadata = messageProcessingFailedEvent.getPayload().getFailedMessageMetadata();
        assertEquals("id", failedMessageMetadata.getEventId());
        assertEquals("idempotence-id", failedMessageMetadata.getIdempotenceId());
        assertEquals(Instant.MIN, failedMessageMetadata.getCreated());
        assertEquals("test-system", failedMessageMetadata.getSystem());
        assertEquals("test-service", failedMessageMetadata.getService());
        assertEquals("TestType", failedMessageMetadata.getMessageTypeName());
        assertEquals("3.0.3", failedMessageMetadata.getMessageTypeVersion());
        assertEquals(ByteBuffer.wrap(headerValue), failedMessageMetadata.getHeaders().get("the-header"));
    }

    @Test
    void metadata_noHeader() {
        MessageProcessingFailedEvent messageProcessingFailedEvent = MessageProcessingFailedEventBuilder.create()
                .eventHandleException(eventHandleException)
                .serviceName("service")
                .systemName("system")
                .originalMessage(originalJeapMessage, originalJeapMessage.value(), "the-header")
                .build();

        assertTrue(messageProcessingFailedEvent.getPayload().getOptionalFailedMessageMetadata().isPresent());
        FailedMessageMetadata failedMessageMetadata = messageProcessingFailedEvent.getPayload().getFailedMessageMetadata();
        assertEquals("id", failedMessageMetadata.getEventId());
        assertEquals("idempotence-id", failedMessageMetadata.getIdempotenceId());
        assertEquals(Instant.MIN, failedMessageMetadata.getCreated());
        assertEquals("test-system", failedMessageMetadata.getSystem());
        assertEquals("test-service", failedMessageMetadata.getService());
        assertEquals("TestType", failedMessageMetadata.getMessageTypeName());
        assertEquals("3.0.3", failedMessageMetadata.getMessageTypeVersion());
        assertTrue(failedMessageMetadata.getHeaders().isEmpty());
    }

}
