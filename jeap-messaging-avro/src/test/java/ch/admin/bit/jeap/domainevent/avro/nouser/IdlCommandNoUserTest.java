package ch.admin.bit.jeap.domainevent.avro.nouser;

import ch.admin.bit.jeap.command.Command;
import ch.admin.bit.jeap.domainevent.avro.command.idl.IdlTestNoUserCommand;
import ch.admin.bit.jeap.messaging.avro.AvroSerializationHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdlCommandNoUserTest {

    @Test
    void create() {
        IdlTestNoUserCommand target = new IdlTestNoUserCommand();
        assertNotNull(target);
    }

    @Test
    void extendsInterface() {
        assertTrue(Command.class.isAssignableFrom(IdlTestNoUserCommand.class),
                IdlTestNoUserCommand.class + " does not extend " + Command.class);
    }

    @Test
    void builder() {
        IdlTestNoUserCommand target = IdlCommandNoUserBuilder.create()
                .idempotenceId("ID-123")
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
        assertEquals("IdlTestNoUserCommand", target.getType().getName());
        assertEquals("1.0.0", target.getType().getVersion());
        assertTrue(target.getOptionalPayload().isPresent());
        assertNotNull(target.getReferences());
        assertEquals("referenceId", target.getReferences().getCustomReference().getReferenceId());
        assertEquals("customId", target.getReferences().getOtherReference().getCustomId());
        assertFalse(target.getOptionalUser().isPresent());
    }

    @Test
    void serializationTest() throws Exception {
        IdlTestNoUserCommand target = IdlCommandNoUserBuilder.create()
                .idempotenceId("idempotenceId")
                .build();
        byte[] serialized = AvroSerializationHelper.serialize(target);
        IdlTestNoUserCommand result = AvroSerializationHelper.deserialize(serialized, IdlTestNoUserCommand.class);
        assertEquals(target, result);
    }

}
