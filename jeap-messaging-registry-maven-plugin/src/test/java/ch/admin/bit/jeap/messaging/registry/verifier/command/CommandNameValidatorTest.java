package ch.admin.bit.jeap.messaging.registry.verifier.command;

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

class CommandNameValidatorTest {
    private final JsonNodeFactory factory = JsonNodeFactory.instance;

    @Test
    void notSystem() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("commandName", new TextNode("SomethingTestCommand")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("system")
                .messageTypeName("SomethingTestCommand")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = CommandNameValidator.validate(validationContext, jsonNode);

        Assertions.assertFalse(result.isValid(), "Command name does not start with system");
    }

    @Test
    void systemJeap() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("commandName", new TextNode("SomethingTestCommand")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("jeap")
                .messageTypeName("SomethingTestCommand")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = CommandNameValidator.validate(validationContext, jsonNode);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void systemNotCapitalized() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("commandName", new TextNode("SomethingTestCommand")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("system")
                .messageTypeName("systemTestCommand")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = CommandNameValidator.validate(validationContext, jsonNode);

        Assertions.assertFalse(result.isValid(), "Command name not capitalized");
    }

    @Test
    void notCommand() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("commandName", new TextNode("SomethingTestCommand")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("system")
                .messageTypeName("SystemTestCommand")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = CommandNameValidator.validate(validationContext, jsonNode);

        Assertions.assertFalse(result.isValid(), "Command name does not end with Command");
    }

    @Test
    void notSameInFilename() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("commandName", new TextNode("SomethingElseCommand")));
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
        JsonNode jsonNode = new ObjectNode(factory, Map.of("commandName", new TextNode("SystemTestCommand")));
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
        JsonNode jsonNode = new ObjectNode(factory, Map.of("commandName", new TextNode("SharedTestCommand")));
        ValidationContext validationContext = ValidationContext.builder()
                .systemName("_shared")
                .messageTypeName("SharedTestCommand")
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = CommandNameValidator.validate(validationContext, jsonNode);

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }
}
