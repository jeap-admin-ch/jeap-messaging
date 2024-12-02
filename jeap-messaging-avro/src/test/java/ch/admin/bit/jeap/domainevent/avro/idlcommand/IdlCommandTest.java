package ch.admin.bit.jeap.domainevent.avro.idlcommand;

import ch.admin.bit.jeap.command.Command;
import ch.admin.bit.jeap.domainevent.avro.command.idl.IdlTestCommand;
import ch.admin.bit.jeap.messaging.avro.AvroMessageUser;
import ch.admin.bit.jeap.messaging.avro.AvroSerializationHelper;
import ch.admin.bit.jeap.messaging.model.MessageUser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IdlCommandTest {

    private static final AvroMessageUser USER = AvroMessageUser.newBuilder()
            .setFamilyName("Muster")
            .setGivenName("Hans")
            .setId("some-id-111")
            .setBusinessPartnerName("Restaurant Sternen")
            .setBusinessPartnerId("some-bpid-234235")
            .setPropertiesMap(Map.of("key-1", "value-1", "key-2", "value-2"))
            .build();

    @Test
    void create() {
        IdlTestCommand target = new IdlTestCommand();
        assertNotNull(target);
    }

    @Test
    void extendsInterface() {
        assertTrue(Command.class.isAssignableFrom(IdlTestCommand.class),
                IdlTestCommand.class + " does not extend " + Command.class);
    }

    @Test
    void builder() {
        IdlTestCommand target = IdlCommandBuilder.create()
                .idempotenceId("ID-123")
                .user(USER)
                .build();
        assertNotNull(target);
        assertNotNull(target.getIdentity());
        assertNotNull(target.getIdentity().getId());
        assertEquals("ID-123", target.getIdentity().getIdempotenceId());
        assertNotNull(target.getIdentity().getCreated(), "Wrong timestamp");
        assertNotNull(target.getPublisher());
        assertEquals("DomainEventTest", target.getPublisher().getSystem());
        assertEquals("IdlTest", target.getPublisher().getService());
        assertNotNull(target.getType());
        assertEquals("IdlTestCommand", target.getType().getName());
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
        IdlTestCommand target = IdlCommandBuilder.create()
                .idempotenceId("ID-123")
                .build();
        assertFalse(target.getOptionalUser().isPresent());
    }

    @Test
    void serializationTest() throws Exception {
        IdlTestCommand target = IdlCommandBuilder.create()
                .idempotenceId("idempotenceId")
                .build();
        byte[] serialized = AvroSerializationHelper.serialize(target);
        IdlTestCommand result = AvroSerializationHelper.deserialize(serialized, IdlTestCommand.class);
        assertEquals(target, result);
    }

    @Test
    void serializationWithoutExplicitFieldsTest() throws Exception {
        AvroMessageUser user = AvroMessageUser.newBuilder()
                .setPropertiesMap(Map.of("key-1", "value-1", "key-2", "value-2"))
                .build();
        IdlTestCommand target = IdlCommandBuilder.create()
                .idempotenceId("idempotenceId")
                .user(user)
                .build();
        byte[] serialized = AvroSerializationHelper.serialize(target);
        IdlTestCommand result = AvroSerializationHelper.deserialize(serialized, IdlTestCommand.class);
        assertEquals(target, result);
    }

    @Test
    void serializationWithOnlyExplicitFieldsTest() throws Exception {
        AvroMessageUser user = AvroMessageUser.newBuilder()
                .setFamilyName("Muster")
                .setGivenName("Hans")
                .setId("some-id-111")
                .build();
        IdlTestCommand target = IdlCommandBuilder.create()
                .idempotenceId("idempotenceId")
                .user(user)
                .build();
        byte[] serialized = AvroSerializationHelper.serialize(target);
        IdlTestCommand result = AvroSerializationHelper.deserialize(serialized, IdlTestCommand.class);
        assertEquals(target, result);
    }
}
