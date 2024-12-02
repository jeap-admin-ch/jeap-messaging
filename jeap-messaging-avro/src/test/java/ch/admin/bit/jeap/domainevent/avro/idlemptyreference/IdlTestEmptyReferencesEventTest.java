package ch.admin.bit.jeap.domainevent.avro.idlemptyreference;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.domainevent.avro.event.idl.IdlTestEvent;
import ch.admin.bit.jeap.domainevent.avro.event.idlemptyreferences.IdlTestEmptyReferencesEvent;
import ch.admin.bit.jeap.messaging.avro.AvroSerializationHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdlTestEmptyReferencesEventTest {

    @Test
    void create() {
        IdlTestEmptyReferencesEvent target = new IdlTestEmptyReferencesEvent();
        assertNotNull(target);
    }

    @Test
    void extendsInterface() {
        assertTrue(DomainEvent.class.isAssignableFrom(IdlTestEmptyReferencesEvent.class),
                IdlTestEvent.class + " does not extend " + DomainEvent.class);
    }

    @Test
    void builder() {
        IdlTestEmptyReferencesEvent target = IdlTestEmptyReferencesEventBuilder.create()
                .idempotenceId("ID-123")
                .build();
        assertNotNull(target);
        assertNotNull(target.getIdentity());
        assertNotNull(target.getIdentity().getEventId());
        assertEquals("ID-123", target.getIdentity().getIdempotenceId());
        assertNotNull(target.getIdentity().getCreated(), "Wrong timestamp");
        assertNotNull(target.getPublisher());
        assertEquals("DomainEventTest", target.getPublisher().getSystem());
        assertEquals("IdlEmptyReferencesTest", target.getPublisher().getService());
        assertNotNull(target.getType());
        assertEquals("IdlTestEmptyReferencesEvent", target.getType().getName());
        assertEquals("1.0.0", target.getType().getVersion());
        assertTrue(target.getOptionalPayload().isPresent());
        assertNotNull(target.getReferences());
    }

    @Test
    void serializationTest() throws Exception {
        IdlTestEmptyReferencesEvent target = IdlTestEmptyReferencesEventBuilder.create()
                .idempotenceId("idempotenceId")
                .build();
        byte[] serialized = AvroSerializationHelper.serialize(target);
        IdlTestEmptyReferencesEvent result = AvroSerializationHelper.deserialize(serialized, IdlTestEmptyReferencesEvent.class);
        assertEquals(target, result);
    }
}
