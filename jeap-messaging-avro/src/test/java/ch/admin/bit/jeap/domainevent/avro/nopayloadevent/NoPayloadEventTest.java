package ch.admin.bit.jeap.domainevent.avro.nopayloadevent;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.domainevent.avro.event.nopayload.NoPayloadTestEvent;
import ch.admin.bit.jeap.messaging.avro.AvroSerializationHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoPayloadEventTest {

    @Test
    void create() {
        NoPayloadTestEvent target = new NoPayloadTestEvent();
        assertNotNull(target);
    }

    @Test
    void extendsInterface() {
        assertTrue(DomainEvent.class.isAssignableFrom(NoPayloadTestEvent.class),
                NoPayloadTestEvent.class + " does not extend " + DomainEvent.class);
    }

    @Test
    void builder() {
        NoPayloadTestEvent target = NoPayloadEventBuilder.create()
                .idempotenceId("ID-123")
                .build();
        assertNotNull(target);
        assertNotNull(target.getIdentity());
        assertNotNull(target.getIdentity().getEventId());
        assertEquals("ID-123", target.getIdentity().getIdempotenceId());
        assertNotNull(target.getIdentity().getCreated(), "Wrong timestamp");
        assertNotNull(target.getPublisher());
        assertEquals("DomainEventTest", target.getPublisher().getSystem());
        assertEquals("NoPayloadTest", target.getPublisher().getService());
        assertNotNull(target.getType());
        assertEquals("NoPayloadTestEvent", target.getType().getName());
        assertEquals("1.0.0", target.getType().getVersion());
        assertNotNull(target.getReferences());
        assertTrue(target.getOptionalPayload().isEmpty());
        assertEquals("referenceId", target.getReferences().getReference1().getReferenceId());
    }

    @Test
    void serializationTest() throws Exception {
        NoPayloadTestEvent target = NoPayloadEventBuilder.create()
                .idempotenceId("idempotenceId")
                .build();
        byte[] serialized = AvroSerializationHelper.serialize(target);
        NoPayloadTestEvent result = AvroSerializationHelper.deserialize(serialized, NoPayloadTestEvent.class);
        assertEquals(target, result);
    }
}
