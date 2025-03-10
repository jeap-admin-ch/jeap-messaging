package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message;

import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.JmeEnumTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;

import java.util.UUID;

public class TestMessages {

    public static JmeSimpleTestEvent createJmeSimpleTestEvent(UUID contextId) {
        return JmeSimpleTestEventBuilder.create()
                .idempotenceId(randomIdempotenceIdString())
                .message("test")
                .processId(contextId.toString())
                .build();
    }

    public static JmeDeclarationCreatedEvent createDeclarationCreatedEvent(UUID contextId) {
        return createDeclarationCreatedEvent(randomIdempotenceId(), contextId, "test");
    }

    public static JmeDeclarationCreatedEvent createDeclarationCreatedEvent(String message) {
        return createDeclarationCreatedEvent(randomIdempotenceId(), randomContextId(), message);
    }

    public static JmeDeclarationCreatedEvent createDeclarationCreatedEvent(UUID idempotenceId, UUID contextId) {
        return createDeclarationCreatedEvent(idempotenceId, contextId, "test");
    }

    private static JmeDeclarationCreatedEvent createDeclarationCreatedEvent(UUID idempotenceId, UUID contextId, String message) {
        return JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(idempotenceId.toString())
                .message(message)
                .processId(contextId.toString())
                .build();
    }

    public static JmeEnumTestEvent createEnumTestEvent() {
        return createEnumTestEvent(randomContextId());
    }

    public static JmeEnumTestEvent createEnumTestEvent(UUID contextId) {
        return JmeEnumTestEventBuilder.create()
                .idempotenceId(randomIdempotenceIdString())
                .message("test")
                .processId(contextId.toString())
                .build();
    }

    public static UUID randomContextId() {
        return UUID.randomUUID();
    }

    public static UUID randomIdempotenceId() {
        return UUID.randomUUID();
    }

    public static String randomIdempotenceIdString() {
        return UUID.randomUUID().toString();
    }

}
