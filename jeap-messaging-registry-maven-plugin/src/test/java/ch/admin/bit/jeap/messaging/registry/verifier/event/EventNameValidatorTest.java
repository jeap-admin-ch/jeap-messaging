package ch.admin.bit.jeap.messaging.registry.verifier.event;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Map;
import java.util.stream.Stream;

class EventNameValidatorTest {
    private final JsonNodeFactory factory = JsonNodeFactory.instance;

    @ParameterizedTest(name = "{0}")
    @MethodSource("eventNameValidationCases")
    void validateEventName(String testCase, String systemName, String messageTypeName, boolean expectedValid) {
        ValidationResult result = EventNameValidator.validate(validationContext(systemName, messageTypeName),
                eventDescriptor());

        Assertions.assertEquals(expectedValid, result.isValid(), String.join(",", result.getErrors()));
    }

    private static Stream<Arguments> eventNameValidationCases() {
        return Stream.of(
                Arguments.of("event name must start with the system name", "system", "SomethingTestEvent", false),
                Arguments.of("jeap events may omit the system prefix", "jeap", "SomethingTestEvent", true),
                Arguments.of("event name must use a capitalized system prefix", "system", "systemTestEvent", false),
                Arguments.of("event name must end with Event", "system", "SystemTestCommand", false)
        );
    }

    @Test
    void notSameInFilename() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("eventName", TextNode.valueOf("SomethingElseEvent")));
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
        JsonNode jsonNode = new ObjectNode(factory, Map.of("eventName", TextNode.valueOf("SystemTestEvent")));
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
        JsonNode jsonNode = new ObjectNode(factory, Map.of("eventName", TextNode.valueOf("SharedTestEvent")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("_shared")
                .messageTypeName("SharedTestEvent")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = EventNameValidator.validate(validationContext, jsonNode);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    private JsonNode eventDescriptor() {
        return new ObjectNode(factory, Map.of("eventName", TextNode.valueOf("SomethingTestEvent")));
    }

    private static ValidationContext validationContext(String systemName, String messageTypeName) {
        return ValidationContext.builder()
                .systemName(systemName)
                .messageTypeName(messageTypeName)
                .descriptorFile(new File("test"))
                .build();
    }
}
