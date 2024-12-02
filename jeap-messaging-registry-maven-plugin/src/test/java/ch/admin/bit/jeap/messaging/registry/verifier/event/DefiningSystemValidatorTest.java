package ch.admin.bit.jeap.messaging.registry.verifier.event;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.helper.MessagingType;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.messaging.registry.verifier.common.DefiningSystemValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

class DefiningSystemValidatorTest {
    private final JsonNodeFactory factory = JsonNodeFactory.instance;

    @Test
    void invalidDefiningSystem() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("definingSystem", new TextNode("Something")));
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
        JsonNode jsonNode = new ObjectNode(factory, Map.of("definingSystem", new TextNode("Test")));
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
        JsonNode jsonNode = new ObjectNode(factory, Map.of("definingSystem", new TextNode("TEST")));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .systemName("test")
                .messagingType(MessagingType.EVENT)
                .build();

        ValidationResult result = DefiningSystemValidator.validate(validationContext, jsonNode);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }
}
