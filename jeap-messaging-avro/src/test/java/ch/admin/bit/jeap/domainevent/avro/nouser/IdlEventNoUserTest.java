package ch.admin.bit.jeap.domainevent.avro.nouser;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.domainevent.avro.event.idl.IdlTestNoUserEvent;
import ch.admin.bit.jeap.messaging.avro.AvroSerializationHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdlEventNoUserTest {

    @Test
    void create() {
        IdlTestNoUserEvent target = new IdlTestNoUserEvent();
        assertNotNull(target);
    }

    @Test
    void extendsInterface() {
        assertTrue(DomainEvent.class.isAssignableFrom(IdlTestNoUserEvent.class),
                IdlTestNoUserEvent.class + " does not extend " + DomainEvent.class);
    }

    @Test
    void builder() {
        IdlTestNoUserEvent target = IdlEventNoUserBuilder.create()
                .idempotenceId("ID-123")
                .build();
        assertNotNull(target);
        assertNotNull(target.getIdentity());
        assertNotNull(target.getIdentity().getEventId());
        assertEquals("ID-123", target.getIdentity().getIdempotenceId());
        assertNotNull(target.getIdentity().getCreated(), "Wrong timestamp");
        assertNotNull(target.getPublisher());
        assertEquals("DomainEventTest", target.getPublisher().getSystem());
        assertEquals("IdlTest", target.getPublisher().getService());
        assertNotNull(target.getType());
        assertEquals("IdlTestNoUserEvent", target.getType().getName());
        assertEquals("1.0.0", target.getType().getVersion());
        assertTrue(target.getOptionalPayload().isPresent());
        assertNotNull(target.getReferences());
        assertEquals("referenceId", target.getReferences().getCustomReference().getReferenceId());
        assertEquals("customId", target.getReferences().getOtherReference().getCustomId());
        assertFalse(target.getOptionalUser().isPresent());
    }

    @Test
    void serializationTest() throws Exception {
        IdlTestNoUserEvent target = IdlEventNoUserBuilder.create()
                .idempotenceId("idempotenceId")
                .build();
        byte[] serialized = AvroSerializationHelper.serialize(target);
        IdlTestNoUserEvent result = AvroSerializationHelper.deserialize(serialized, IdlTestNoUserEvent.class);
        assertEquals(target, result);
    }

}
