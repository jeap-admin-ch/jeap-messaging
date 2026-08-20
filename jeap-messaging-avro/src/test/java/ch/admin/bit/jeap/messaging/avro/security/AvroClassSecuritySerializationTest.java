package ch.admin.bit.jeap.messaging.avro.security;

import ch.admin.bit.jeap.domainevent.avro.variant.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroSerializationHelper;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import org.apache.avro.specific.SpecificData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the whitelist against what Avro actually asks for when resolving a generated message class, instead of
 * only asserting the predicate itself.
 */
class AvroClassSecuritySerializationTest {

    @BeforeEach
    void forgetInstalledWhitelist() {
        AvroClassSecurity.reset();
    }

    @AfterEach
    void restoreDefaultWhitelist() {
        AvroClassSecurity.reset();
        AvroClassSecurity.installDefaultIfMissing();
    }

    @Test
    void resolveGeneratedMessageClass_whitelistNotInstalled_isRejectedByAvro() {
        // A fresh SpecificData instance, the shared one caches the classes resolved by the other tests
        SpecificData specificData = new SpecificData();

        SecurityException exception = assertThrows(SecurityException.class,
                () -> specificData.getClass(JmeDeclarationCreatedEvent.getClassSchema()));

        assertTrue(exception.getMessage().contains(JmeDeclarationCreatedEvent.class.getName()), exception.getMessage());
    }

    @Test
    void resolveGeneratedMessageClass_defaultWhitelistInstalled_resolvesTheGeneratedClass() {
        AvroClassSecurity.install(List.of(), List.of());
        SpecificData specificData = new SpecificData();

        assertEquals(JmeDeclarationCreatedEvent.class,
                specificData.getClass(JmeDeclarationCreatedEvent.getClassSchema()));
    }

    @Test
    void serializeAndDeserialize_defaultWhitelistInstalled_roundTripSucceeds() throws Exception {
        AvroClassSecurity.install(List.of(), List.of());
        JmeDeclarationCreatedEvent event = JmeDeclarationCreatedEventBuilder.create()
                .message("test")
                .idempotenceId("idempotenceId")
                .build();

        byte[] serialized = AvroSerializationHelper.serialize(event);
        JmeDeclarationCreatedEvent result =
                AvroSerializationHelper.deserialize(serialized, JmeDeclarationCreatedEvent.class);

        assertEquals(event, result);
    }
}
