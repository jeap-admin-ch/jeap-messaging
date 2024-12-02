package ch.admin.bit.jeap.domainevent.avro.noreferencesevent;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.domainevent.avro.event.noreferencesevent.NoReferencesEvent;
import ch.admin.bit.jeap.messaging.avro.AvroSerializationHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoReferencesEventTest {

    @Test
    void create() {
        NoReferencesEvent target = new NoReferencesEvent();
        assertNotNull(target);
    }

    @Test
    void extendsInterface() {
        assertTrue(DomainEvent.class.isAssignableFrom(NoReferencesEvent.class),
                NoReferencesEvent.class + " does not extend " + DomainEvent.class);
    }

    @Test
    void builder() {
        NoReferencesEvent target = NoReferencesEventBuilder.create()
                .idempotenceId("ID-123")
                .build();
        assertNotNull(target);
        assertNotNull(target.getIdentity());
        assertNotNull(target.getIdentity().getEventId());
        assertEquals("ID-123", target.getIdentity().getIdempotenceId());
        assertNotNull(target.getIdentity().getCreated(), "Wrong timestamp");
        assertNotNull(target.getPublisher());
        assertEquals("DomainEventTest", target.getPublisher().getSystem());
        assertEquals("NoReferencesTest", target.getPublisher().getService());
        assertNotNull(target.getType());
        assertEquals("NoReferencesEvent", target.getType().getName());
        assertEquals("1.0.0", target.getType().getVersion());
        assertTrue(target.getOptionalReferences().isEmpty());
    }

    @Test
    void serializationTest() throws Exception {
        NoReferencesEvent target = NoReferencesEventBuilder.create()
                .idempotenceId("idempotenceId")
                .build();
        byte[] serialized = AvroSerializationHelper.serialize(target);
        NoReferencesEvent result = AvroSerializationHelper.deserialize(serialized, NoReferencesEvent.class);
        assertEquals(target, result);
    }
}
