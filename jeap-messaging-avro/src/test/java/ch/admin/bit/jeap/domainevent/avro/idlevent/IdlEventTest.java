package ch.admin.bit.jeap.domainevent.avro.idlevent;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventUser;
import ch.admin.bit.jeap.domainevent.avro.event.idl.IdlTestEvent;
import ch.admin.bit.jeap.messaging.avro.AvroSerializationHelper;
import ch.admin.bit.jeap.messaging.model.MessageUser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IdlEventTest {

    private static final AvroDomainEventUser USER = AvroDomainEventUser.newBuilder()
            .setFamilyName("Muster")
            .setGivenName("Hans")
            .setId("some-id-111")
            .setBusinessPartnerName("Restaurant Sternen")
            .setBusinessPartnerId("some-bpid-234235")
            .setPropertiesMap(Map.of("key-1", "value-1", "key-2", "value-2"))
            .build();

    @Test
    void create() {
        IdlTestEvent target = new IdlTestEvent();
        assertNotNull(target);
    }

    @Test
    void extendsInterface() {
        assertTrue(DomainEvent.class.isAssignableFrom(IdlTestEvent.class),
                IdlTestEvent.class + " does not extend " + DomainEvent.class);
    }

    @Test
    void builder() {
        IdlTestEvent target = IdlEventBuilder.create()
                .idempotenceId("ID-123")
                .user(USER)
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
        assertEquals("IdlTestEvent", target.getType().getName());
        assertEquals("1.0.0", target.getType().getVersion());
        assertTrue(target.getOptionalPayload().isPresent());
        assertNotNull(target.getReferences());
        assertEquals("referenceId", target.getReferences().getCustomReference().getReferenceId());
        assertEquals("customId", target.getReferences().getOtherReference().getCustomId());
        assertTrue(target.getOptionalUser().isPresent());
        MessageUser userFromEvent = target.getOptionalUser().get();
        assertEquals(USER, userFromEvent);
    }

    @Test
    void builderWithoutUser() {
        IdlTestEvent target = IdlEventBuilder.create()
                .idempotenceId("ID-123")
                .build();
        assertFalse(target.getOptionalUser().isPresent());
    }

    @Test
    void serializationTest() throws Exception {
        IdlTestEvent target = IdlEventBuilder.create()
                .idempotenceId("idempotenceId")
                .user(USER)
                .build();
        byte[] serialized = AvroSerializationHelper.serialize(target);
        IdlTestEvent result = AvroSerializationHelper.deserialize(serialized, IdlTestEvent.class);
        assertEquals(target, result);
    }

    @Test
    void serializationUserWithoutExplicitFieldsTest() throws Exception {
        AvroDomainEventUser USER = AvroDomainEventUser.newBuilder()
                .setPropertiesMap(Map.of("I.am.a.key", "value"))
                .build();
        IdlTestEvent target = IdlEventBuilder.create()
                .idempotenceId("idempotenceId")
                .user(USER)
                .build();
        byte[] serialized = AvroSerializationHelper.serialize(target);
        IdlTestEvent result = AvroSerializationHelper.deserialize(serialized, IdlTestEvent.class);
        assertEquals(target, result);
    }

    @Test
    void serializationUserOnlyExplicitFieldsTest() throws Exception {
        AvroDomainEventUser USER = AvroDomainEventUser.newBuilder()
                .setFamilyName("Muster")
                .setGivenName("Hans")
                .setId("some-id-111")
                .build();
        IdlTestEvent target = IdlEventBuilder.create()
                .idempotenceId("idempotenceId")
                .user(USER)
                .build();
        byte[] serialized = AvroSerializationHelper.serialize(target);
        IdlTestEvent result = AvroSerializationHelper.deserialize(serialized, IdlTestEvent.class);
        assertEquals(target, result);
    }

}
