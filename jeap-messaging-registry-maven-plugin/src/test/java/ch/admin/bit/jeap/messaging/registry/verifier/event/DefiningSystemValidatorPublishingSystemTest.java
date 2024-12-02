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

/**
 * Those tests are to ensure that using "publishingSystem" is still supported. They can be deleted when this
 * does not need to be the case any more
 */
class DefiningSystemValidatorPublishingSystemTest {
    private final JsonNodeFactory factory = JsonNodeFactory.instance;

    @Test
    void invalidPublishingSystem() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("publishingSystem", new TextNode("Something")));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .systemName("test")
                .messagingType(MessagingType.EVENT)
                .build();

        ValidationResult result = DefiningSystemValidator.validate(validationContext, jsonNode);

        Assertions.assertFalse(result.isValid(), "Publishing system is not system name");
    }

    @Test
    void notCapsPublishingSystem() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("publishingSystem", new TextNode("Test")));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .systemName("test")
                .messagingType(MessagingType.EVENT)
                .build();

        ValidationResult result = DefiningSystemValidator.validate(validationContext, jsonNode);

        Assertions.assertFalse(result.isValid(), "Publishing system is not system name");
    }

    @Test
    void validPublishingSystem() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("publishingSystem", new TextNode("TEST")));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .systemName("test")
                .messagingType(MessagingType.EVENT)
                .build();

        ValidationResult result = DefiningSystemValidator.validate(validationContext, jsonNode);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }
}
