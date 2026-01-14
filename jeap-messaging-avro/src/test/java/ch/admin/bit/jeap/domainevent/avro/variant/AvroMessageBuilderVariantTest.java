package ch.admin.bit.jeap.domainevent.avro.variant;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AvroMessageBuilderVariantTest {

    @Test
    void setVariant_messageTypeGeneratedWithoutVariant_throwsException() {
        JmeDeclarationCreatedEventBuilder jmeDeclarationCreatedEventBuilder = JmeDeclarationCreatedEventBuilder.create()
                .message("test")
                .idempotenceId("idempotenceId");
        String expectedErrorMessage = "The schema of the message type 'ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent' does not have a 'variant' field. In order to use the 'variant' field in this message type, a new version of the message must be defined in order to generate the schema again.";
        AvroMessageBuilderException avroMessageBuilderException = assertThrows(AvroMessageBuilderException.class, () -> jmeDeclarationCreatedEventBuilder.variant("variant1"));
        assertEquals(expectedErrorMessage, avroMessageBuilderException.getMessage());
    }

    @Test
    void buildWithoutVariant_messageTypeGeneratedWithoutVariant_buildIsOk() {
        DomainEvent target = JmeDeclarationCreatedEventBuilder.create()
                .message("test")
                .idempotenceId("idempotenceId")
                .build();
        assertNotNull(target.getType());
        assertEquals("JmeDeclarationCreatedEvent", target.getType().getName());
        assertNull(target.getType().getVariant());
    }

    @Test
    void buildWithVariant_messageTypeGeneratedWithVariant_buildIsOk() {
        DomainEvent target = JmeTestCreatedEventBuilder.create()
                .idempotenceId("idempotenceId")
                .variant("variant1")
                .build();
        assertNotNull(target.getType());
        assertEquals("JmeTestCreatedEvent", target.getType().getName());
        assertEquals("variant1", target.getType().getVariant());
    }
}
