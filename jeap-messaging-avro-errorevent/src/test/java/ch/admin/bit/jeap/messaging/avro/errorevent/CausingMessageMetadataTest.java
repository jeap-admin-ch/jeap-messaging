package ch.admin.bit.jeap.messaging.avro.errorevent;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.model.MessageIdentity;
import ch.admin.bit.jeap.messaging.model.MessagePublisher;
import ch.admin.bit.jeap.messaging.model.MessageType;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CausingMessageMetadataTest {

    private static final String ID = "ad85fba4-5bf5-4ae1-ae56-6d23d542644f";
    private static final String IDEMPOTENCE_ID = "c9867248-95de-4d1d-a721-0c6dd808951d";
    private static final Instant CREATED = Instant.now();

    private static final String SYSTEM = "mysystem";
    private static final String SERVICE = "myservice";

    private static final String MESSAGE_TYPE_NAME = "myMessageTypeName";
    private static final String MESSAGE_TYPE_VERSION = "1.0.0";

    @Test
    void allSet() {
        MessageIdentity identity = mock(MessageIdentity.class);
        when(identity.getId()).thenReturn(ID);
        when(identity.getIdempotenceId()).thenReturn(IDEMPOTENCE_ID);
        when(identity.getCreated()).thenReturn(CREATED);

        MessagePublisher publisher = mock(MessagePublisher.class);
        when(publisher.getSystem()).thenReturn(SYSTEM);
        when(publisher.getService()).thenReturn(SERVICE);

        MessageType messageType = mock(MessageType.class);
        when(messageType.getName()).thenReturn(MESSAGE_TYPE_NAME);
        when(messageType.getVersion()).thenReturn(MESSAGE_TYPE_VERSION);

        Message message = mock(Message.class);

        when(message.getIdentity()).thenReturn(identity);
        when(message.getPublisher()).thenReturn(publisher);
        when(message.getType()).thenReturn(messageType);

        Headers headers = new RecordHeaders(Arrays.asList(
                new RecordHeader("header1", "value1".getBytes()),
                new RecordHeader("header2", "value2".getBytes()),
                new RecordHeader("header3", "value3".getBytes())
        ));

        CausingMessageMetadata result = CausingMessageMetadata.from(message, headers, "header1", "header2");

        assertNotNull(result);
        assertEquals(ID, result.eventId());
        assertEquals(IDEMPOTENCE_ID, result.idempotenceId());
        assertEquals(CREATED, result.created());
        assertEquals(SYSTEM, result.system());
        assertEquals(SERVICE, result.service());
        assertEquals(MESSAGE_TYPE_NAME, result.messageTypeName());
        assertEquals(MESSAGE_TYPE_VERSION, result.messageTypeVersion());
        assertNotNull(result.headers());
        assertEquals(2, result.headers().size());
        assertNotNull(result.headers().get("header1"));
        assertNotNull(result.headers().get("header2"));
        assertEquals("value1", new String(result.headers().get("header1").array()));
        assertEquals("value2", new String(result.headers().get("header2").array()));
    }

    @Test
    void nothingSet() {
        CausingMessageMetadata result = CausingMessageMetadata.from(null, new RecordHeaders(), "header1", "header2");

        assertNull(result);
    }

    @Test
    void headersOnlySet() {
        Headers headers = new RecordHeaders(Arrays.asList(
                new RecordHeader("header1", "value1".getBytes()),
                new RecordHeader("header2", "value2".getBytes()),
                new RecordHeader("header3", "value3".getBytes())
        ));

        CausingMessageMetadata result = CausingMessageMetadata.from(null, headers, "header1", "header2");

        assertNotNull(result);
        assertNull(result.eventId());
        assertNull(result.idempotenceId());
        assertNull(result.created());
        assertNull(result.system());
        assertNull(result.service());
        assertNull(result.messageTypeName());
        assertNull(result.messageTypeVersion());
        assertNotNull(result.headers());
        assertEquals(2, result.headers().size());
        assertNotNull(result.headers().get("header1"));
        assertNotNull(result.headers().get("header2"));
        assertEquals("value1", new String(result.headers().get("header1").array()));
        assertEquals("value2", new String(result.headers().get("header2").array()));

    }

    @Test
    void messageOnlySet() {
        MessageIdentity identity = mock(MessageIdentity.class);
        when(identity.getId()).thenReturn(ID);
        when(identity.getIdempotenceId()).thenReturn(IDEMPOTENCE_ID);
        when(identity.getCreated()).thenReturn(CREATED);

        MessagePublisher publisher = mock(MessagePublisher.class);
        when(publisher.getSystem()).thenReturn(SYSTEM);
        when(publisher.getService()).thenReturn(SERVICE);

        MessageType messageType = mock(MessageType.class);
        when(messageType.getName()).thenReturn(MESSAGE_TYPE_NAME);
        when(messageType.getVersion()).thenReturn(MESSAGE_TYPE_VERSION);

        Message message = mock(Message.class);

        when(message.getIdentity()).thenReturn(identity);
        when(message.getPublisher()).thenReturn(publisher);
        when(message.getType()).thenReturn(messageType);

        CausingMessageMetadata result = CausingMessageMetadata.from(message, new RecordHeaders(), "header1", "header2");

        assertNotNull(result);
        assertEquals(ID, result.eventId());
        assertEquals(IDEMPOTENCE_ID, result.idempotenceId());
        assertEquals(CREATED, result.created());
        assertEquals(SYSTEM, result.system());
        assertEquals(SERVICE, result.service());
        assertEquals(MESSAGE_TYPE_NAME, result.messageTypeName());
        assertEquals(MESSAGE_TYPE_VERSION, result.messageTypeVersion());
        assertNotNull(result.headers());
        assertEquals(0, result.headers().size());
    }

}
