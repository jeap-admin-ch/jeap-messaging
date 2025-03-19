package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message;

import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.JmeEnumTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;

import java.util.UUID;

public class TestMessages {

    public static JmeSimpleTestEvent createJmeSimpleTestEvent(UUID contextId) {
        return createJmeSimpleTestEvent(contextId, "test");
    }

    public static JmeSimpleTestEvent createJmeSimpleTestEvent(UUID contextId, String serviceName) {
        return JmeSimpleTestEventBuilder.create()
                .idempotenceId(randomIdempotenceIdString())
                .message("test")
                .processId(contextId.toString())
                .serviceName(serviceName)
                .build();
    }

    public static JmeSimpleTestEvent createJmeSimpleTestEvent(UUID contextId, IceCreamFlavour flavour) {
        return JmeSimpleTestEventBuilder.create()
                .idempotenceId(randomIdempotenceIdString())
                .message(flavour.name())
                .processId(contextId.toString())
                .serviceName("test")
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

    public static JmeDeclarationCreatedEvent createDeclarationCreatedEvent(UUID contextId, String serviceName) {
        return createDeclarationCreatedEvent(randomIdempotenceId(), contextId, "test", serviceName);
    }

    private static JmeDeclarationCreatedEvent createDeclarationCreatedEvent(UUID idempotenceId, UUID contextId, String message) {
        return JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(idempotenceId.toString())
                .message(message)
                .processId(contextId.toString())
                .build();
    }

    private static JmeDeclarationCreatedEvent createDeclarationCreatedEvent(UUID idempotenceId, UUID contextId, String message, String serviceName) {
        return JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(idempotenceId.toString())
                .message(message)
                .processId(contextId.toString())
                .serviceName(serviceName)
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
