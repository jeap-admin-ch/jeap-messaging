package ch.admin.bit.jeap.domainevent.avro.schema;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventUser;
import ch.admin.bit.jeap.domainevent.avro.event.schema.AvroSchemaTestEvent;
import ch.admin.bit.jeap.messaging.avro.AvroSerializationHelper;
import ch.admin.bit.jeap.messaging.model.MessageUser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AvroSchemaTestEventTest {

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
        AvroSchemaTestEvent target = new AvroSchemaTestEvent();
        assertNotNull(target);
    }

    @Test
    void extendsInterface() {
        assertTrue(DomainEvent.class.isAssignableFrom(AvroSchemaTestEvent.class),
                AvroSchemaTestEvent.class + " does not extend " + DomainEvent.class);
    }

    @Test
    void builder() {
        AvroSchemaTestEvent target = AvroSchemaTestEventBuilder.create()
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
        assertEquals("AvroSchemaTestEventTest", target.getPublisher().getService());
        assertNotNull(target.getType());
        assertEquals("AvroSchemaTestEvent", target.getType().getName());
        assertEquals("1.0.0", target.getType().getVersion());
        assertTrue(target.getOptionalPayload().isPresent());
        assertNotNull(target.getReferences());
        assertEquals("testId", target.getReferences().getReference1().getTestId());
        assertTrue(target.getOptionalUser().isPresent());
        MessageUser userFromEvent = target.getOptionalUser().get();
        assertEquals(USER, userFromEvent);
    }

    @Test
    void builderWithoutUser() {
        AvroSchemaTestEvent target = AvroSchemaTestEventBuilder.create()
                .idempotenceId("ID-123")
                .build();
        assertFalse(target.getOptionalUser().isPresent());
    }

    @Test
    void serializationTest() throws Exception {
        AvroSchemaTestEvent target = AvroSchemaTestEventBuilder.create()
                .idempotenceId("idempotenceId")
                .user(USER)
                .build();
        byte[] serialized = AvroSerializationHelper.serialize(target);
        AvroSchemaTestEvent result = AvroSerializationHelper.deserialize(serialized, AvroSchemaTestEvent.class);
        assertEquals(target, result);
    }

}

