package ch.admin.bit.jeap.messaging.registry.verifier.event;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

class EventNameValidatorTest {
    private final JsonNodeFactory factory = JsonNodeFactory.instance;

    @Test
    void notSystem() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("eventName", new TextNode("SomethingTestEvent")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("system")
                .messageTypeName("SomethingTestEvent")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = EventNameValidator.validate(validationContext, jsonNode);

        Assertions.assertFalse(result.isValid(), "Event name does not start with system");
    }

    @Test
    void systemJeap() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("eventName", new TextNode("SomethingTestEvent")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("jeap")
                .messageTypeName("SomethingTestEvent")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = EventNameValidator.validate(validationContext, jsonNode);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void systemNotCapitalized() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("eventName", new TextNode("SomethingTestEvent")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("system")
                .messageTypeName("systemTestEvent")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = EventNameValidator.validate(validationContext, jsonNode);

        Assertions.assertFalse(result.isValid(), "Event name not capitalized");
    }

    @Test
    void notEvent() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("eventName", new TextNode("SomethingTestEvent")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("system")
                .messageTypeName("SystemTestCommand")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = EventNameValidator.validate(validationContext, jsonNode);

        Assertions.assertFalse(result.isValid(), "Event name does not end with Event");
    }

    @Test
    void notSameInFilename() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("eventName", new TextNode("SomethingElseEvent")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("system")
                .messageTypeName("SystemTestEvent")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = EventNameValidator.validate(validationContext, jsonNode);

        Assertions.assertFalse(result.isValid(), "Event name in file not equals to event name");
    }

    @Test
    void valid() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("eventName", new TextNode("SystemTestEvent")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("system")
                .messageTypeName("SystemTestEvent")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = EventNameValidator.validate(validationContext, jsonNode);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void sharedEvent() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("eventName", new TextNode("SharedTestEvent")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("_shared")
                .messageTypeName("SharedTestEvent")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = EventNameValidator.validate(validationContext, jsonNode);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }
}
