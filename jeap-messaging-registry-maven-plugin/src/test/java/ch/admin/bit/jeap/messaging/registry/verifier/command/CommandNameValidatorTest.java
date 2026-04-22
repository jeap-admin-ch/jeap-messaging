package ch.admin.bit.jeap.messaging.registry.verifier.command;

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

class CommandNameValidatorTest {
    private final JsonNodeFactory factory = JsonNodeFactory.instance;

    @ParameterizedTest(name = "{0}")
    @MethodSource("commandNameValidationCases")
    void validateCommandName(String testCase, String systemName, String messageTypeName, boolean expectedValid) {
        ValidationResult result = CommandNameValidator.validate(validationContext(systemName, messageTypeName),
                commandDescriptor());

        Assertions.assertEquals(expectedValid, result.isValid(), String.join(",", result.getErrors()));
    }

    private static Stream<Arguments> commandNameValidationCases() {
        return Stream.of(
                Arguments.of("command name must start with the system name", "system", "SomethingTestCommand", false),
                Arguments.of("jeap commands may omit the system prefix", "jeap", "SomethingTestCommand", true),
                Arguments.of("command name must use a capitalized system prefix", "system", "systemTestCommand", false),
                Arguments.of("command name must end with Command", "system", "SystemTestEvent", false)
        );
    }

    @Test
    void notSameInFilename() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("commandName", TextNode.valueOf("SomethingElseCommand")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("system")
                .messageTypeName("SystemTestCommand")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = CommandNameValidator.validate(validationContext, jsonNode);

        Assertions.assertFalse(result.isValid(), "Command name in file not equals to command name");
    }

    @Test
    void valid() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("commandName", TextNode.valueOf("SystemTestCommand")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("system")
                .messageTypeName("SystemTestCommand")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = CommandNameValidator.validate(validationContext, jsonNode);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void sharedSystem() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("commandName", TextNode.valueOf("SharedTestCommand")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("_shared")
                .messageTypeName("SharedTestCommand")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = CommandNameValidator.validate(validationContext, jsonNode);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    private JsonNode commandDescriptor() {
        return new ObjectNode(factory, Map.of("commandName", TextNode.valueOf("SomethingTestCommand")));
    }

    private static ValidationContext validationContext(String systemName, String messageTypeName) {
        return ValidationContext.builder()
                .systemName(systemName)
                .messageTypeName(messageTypeName)
                .descriptorFile(new File("test"))
                .build();
    }
}
