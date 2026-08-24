package ch.admin.bit.jeap.messaging.registry.verifier.event;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.helper.MessagingType;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.messaging.registry.verifier.common.DefiningSystemValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import java.io.File;
import java.util.Map;

class DefiningSystemValidatorTest {
    private final JsonNodeFactory factory = JsonNodeFactory.instance;

    @Test
    void invalidDefiningSystem() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("definingSystem", StringNode.valueOf("Something")));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .systemName("test")
                .messagingType(MessagingType.EVENT)
                .build();

        ValidationResult result = DefiningSystemValidator.validate(validationContext, jsonNode);

        Assertions.assertFalse(result.isValid(), "Defining system is not system name");
    }

    @Test
    void notCapsDefiningSystem() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("definingSystem", StringNode.valueOf("Test")));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .systemName("test")
                .messagingType(MessagingType.EVENT)
                .build();

        ValidationResult result = DefiningSystemValidator.validate(validationContext, jsonNode);

        Assertions.assertFalse(result.isValid(), "Publishing system is not system name");
    }

    @Test
    void validDefiningSystem() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("definingSystem", StringNode.valueOf("TEST")));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .systemName("test")
                .messagingType(MessagingType.EVENT)
                .build();

        ValidationResult result = DefiningSystemValidator.validate(validationContext, jsonNode);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }
}
