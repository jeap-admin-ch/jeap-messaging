package ch.admin.bit.jeap.messaging.registry.verifier.event;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.helper.MessagingType;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.messaging.registry.verifier.common.AvroSchemaValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AvroSchemaValidatorTest {
    private static final JsonNodeFactory factory = JsonNodeFactory.instance;
    private final static File TESTEVENT_RESOURCES_DIR = new File("src/test/resources/valid/descriptor/test/event/testtestevent/");
    private final static File EVENT_RESOURCES_DIR = new File("src/test/resources/commonData/descriptor/test/event");
    private final static File SYSTEM_RESOURCES_DIR = new File("src/test/resources/commonData/descriptor/test");
    private final static File AVRO_DIR = new File("../domainevent-avro/src/main/avro");
    private Optional<JsonNode> oldDescriptor = Optional.of(new ObjectNode(factory));

    @SuppressWarnings("SameParameterValue")
    private static JsonNode versionsWithoutKey(String... versions) {
        return Arrays.stream(versions)
                .map(i -> new ObjectNode(factory, Map.of(
                        "version", new TextNode(i),
                        "compatibilityMode", new TextNode("BACKWARD"),
                        "valueSchema", new TextNode("value_" + i + ".avdl"))))
                .reduce(new ArrayNode(factory), ArrayNode::add, ArrayNode::addAll);
    }

    @SuppressWarnings("SameParameterValue")
    private static JsonNode versionsWithKey(String... versions) {
        return Arrays.stream(versions)
                .map(i -> new ObjectNode(factory, Map.of(
                        "version", new TextNode(i),
                        "compatibilityMode", new TextNode("BACKWARD"),
                        "keySchema", new TextNode("key_" + i + ".avdl"),
                        "valueSchema", new TextNode("value_" + i + ".avdl"))))
                .reduce(new ArrayNode(factory), ArrayNode::add, ArrayNode::addAll);
    }

    @Test
    void noVersions() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of());
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode, oldDescriptor);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void emptyVersions() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("versions", versionsWithoutKey()));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode, oldDescriptor);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void missingValueSchema(@TempDir File tmpDir) {
        JsonNode jsonNode = new ObjectNode(factory, Map.of("versions", versionsWithoutKey("1.1.0")));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .messageTypeDirectory(tmpDir)
                .messagingType(MessagingType.EVENT)
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode, oldDescriptor);

        assertFalse(result.isValid(), "value_1.1.0.avdl is not present");
    }

    @Test
    void invalidValueSchema(@TempDir File tmpDir) throws IOException {
        FileUtils.write(new File(tmpDir, "value_1.1.0.avdl"), "garbage");
        JsonNode jsonNode = new ObjectNode(factory, Map.of("versions", versionsWithoutKey("1.1.0")));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .messageTypeDirectory(tmpDir)
                .messagingType(MessagingType.EVENT)
                .importClassLoader(new ImportClassLoader(AVRO_DIR))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode, oldDescriptor);

        assertFalse(result.isValid(), "value_1.1.0.avdl is not valid");
    }

    @Test
    void missingKeySchema(@TempDir File tmpDir) throws IOException {
        FileUtils.copyFile(new File(TESTEVENT_RESOURCES_DIR, "TestTestEvent_v1.avdl"), new File(tmpDir, "value_1.1.0.avdl"));
        JsonNode jsonNode = new ObjectNode(factory, Map.of("versions", versionsWithKey("1.1.0")));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .messageTypeDirectory(tmpDir)
                .messagingType(MessagingType.EVENT)
                .importClassLoader(new ImportClassLoader(AVRO_DIR))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode, oldDescriptor);

        assertFalse(result.isValid(), "key_1.1.0.avdl is not present");
    }

    @Test
    void firstVersionWithoutKeySchemaSecondVersionWithKeySchema(@TempDir File tmpDir) throws IOException {
        FileUtils.copyFile(new File(TESTEVENT_RESOURCES_DIR, "TestTestEvent_v1.avdl"), new File(tmpDir, "value_1.0.0.avdl"));
        FileUtils.copyFile(new File(TESTEVENT_RESOURCES_DIR, "TestTestEvent_v1.avdl"), new File(tmpDir, "value_2.0.0.avdl"));
        FileUtils.write(new File(tmpDir, "key_2.0.0.avdl"), """
                @namespace("ch.admin.bit.jme.declaration")
                protocol JmeDeclarationCreatedEventKeyProtocol {
                  record BeanReferenceMessageKey {
                    string name;
                  }
                }
                """);
        JsonNode jsonNode = new ObjectNode(factory, Map.of("versions", new ArrayNode(factory,
                List.of(versionsWithoutKey("1.0.0").get(0),
                        versionsWithKey("2.0.0").get(0)))));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .messageTypeDirectory(tmpDir)
                .messagingType(MessagingType.EVENT)
                .messageTypeName("TestTestEvent")
                .importClassLoader(new ImportClassLoader(AVRO_DIR))
                .log(mock(Log.class))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode, oldDescriptor);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void invalidKeySchema(@TempDir File tmpDir) throws IOException {
        FileUtils.copyFile(new File(TESTEVENT_RESOURCES_DIR, "TestTestEvent_v1.avdl"), new File(tmpDir, "value_1.1.0.avdl"));
        FileUtils.write(new File(tmpDir, "key_1.1.0.avdl"), "garbage");
        JsonNode jsonNode = new ObjectNode(factory, Map.of("versions", versionsWithKey("1.1.0")));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .messageTypeDirectory(tmpDir)
                .messagingType(MessagingType.EVENT)
                .importClassLoader(new ImportClassLoader(AVRO_DIR))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode, oldDescriptor);

        assertFalse(result.isValid(), "key_1.1.0.avdl is not valid");
    }

    @Test
    void validSchemas(@TempDir File tmpDir) throws IOException {
        FileUtils.copyFile(new File(TESTEVENT_RESOURCES_DIR, "TestTestEvent_v1.avdl"), new File(tmpDir, "value_1.1.0.avdl"));
        FileUtils.copyFile(new File(TESTEVENT_RESOURCES_DIR, "TestTestEvent_key_v1.avdl"), new File(tmpDir, "key_1.1.0.avdl"));
        JsonNode jsonNode = new ObjectNode(factory, Map.of("versions", versionsWithKey("1.1.0")));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .messageTypeDirectory(tmpDir)
                .messagingType(MessagingType.EVENT)
                .importClassLoader(new ImportClassLoader(AVRO_DIR))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode, oldDescriptor);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void commonKey(@TempDir File tmpDir) throws IOException {
        File descriptorDir = new File(tmpDir, "descriptor");
        File systemDir = new File(descriptorDir, "system");
        File commonSystemDir = new File(systemDir, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(commonSystemDir);
        FileUtils.copyFile(new File(new File(SYSTEM_RESOURCES_DIR, MessageTypeRegistryConstants.COMMON_DIR_NAME), "ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestTestEventKey.avdl"), new File(commonSystemDir, "ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestTestKey.avdl"));
        FileUtils.copyFile(new File(new File(SYSTEM_RESOURCES_DIR, MessageTypeRegistryConstants.COMMON_DIR_NAME), "ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestEnum.avdl"), new File(commonSystemDir, "ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestEnum.avdl"));
        FileUtils.copyFile(new File(new File(SYSTEM_RESOURCES_DIR, MessageTypeRegistryConstants.COMMON_DIR_NAME), "ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestReference.avdl"), new File(commonSystemDir, "ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestReference.avdl"));

        File eventDir = new File(systemDir, "testtestevent");
        FileUtils.forceMkdir(eventDir);
        FileUtils.copyFile(new File(new File(EVENT_RESOURCES_DIR, "testtestevent"), "TestTestEvent_v1.1.0.avdl"), new File(eventDir, "TestTestEvent_v1.1.0.avdl"));

        ArrayNode versions = new ArrayNode(factory);
        versions.add(new ObjectNode(factory, Map.of(
                "version", new TextNode("1.1.0"),
                "valueSchema", new TextNode("TestTestEvent_v1.1.0.avdl"),
                "keySchema", new TextNode("ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestTestKey.avdl"))));
        JsonNode jsonNode = new ObjectNode(factory, Map.of(
                "versions", versions));

        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .messageTypeName("TestTestEvent")
                .messageTypeDirectory(eventDir)
                .messagingType(MessagingType.EVENT)
                .systemDir(systemDir)
                .importClassLoader(new ImportClassLoader(new ImportClassLoader(AVRO_DIR), commonSystemDir, new File("nonexisting")))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode, oldDescriptor);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void commonKeyNotExisting(@TempDir File tmpDir) throws IOException {
        File eventDir = new File(tmpDir, "testtestevent");
        FileUtils.forceMkdir(eventDir);
        FileUtils.copyFile(new File(new File(EVENT_RESOURCES_DIR, "testtestevent"), "TestTestEvent_v1.1.0.avdl"), new File(eventDir, "TestTestEvent_v1.avdl"));

        ArrayNode versions = new ArrayNode(factory);
        versions.add(new ObjectNode(factory, Map.of(
                "version", new IntNode(1),
                "valueSchema", new TextNode("TestTestEvent_v1.avdl"),
                "keySchema", new TextNode("ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestTestKey.avdl"))));
        JsonNode jsonNode = new ObjectNode(factory, Map.of(
                "versions", versions));

        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .messageTypeName("TestTestEvent")
                .messageTypeDirectory(eventDir)
                .messagingType(MessagingType.EVENT)
                .importClassLoader(new ImportClassLoader(AVRO_DIR))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode, oldDescriptor);

        assertFalse(result.isValid(), "Using common key that does not exist. This should fail");
    }

    @Test
    void imports(@TempDir File tmpDir) throws IOException {
        File commonSystemDir = new File(tmpDir, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(commonSystemDir);
        FileUtils.copyFile(
                new File(new File(SYSTEM_RESOURCES_DIR, MessageTypeRegistryConstants.COMMON_DIR_NAME), "ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestReference.avdl"),
                new File(commonSystemDir, "ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestReference.avdl"));
        FileUtils.copyFile(new File(new File(SYSTEM_RESOURCES_DIR, MessageTypeRegistryConstants.COMMON_DIR_NAME), "ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestEnum.avdl"), new File(commonSystemDir, "ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestEnum.avdl"));

        File eventDir = new File(tmpDir, "testtestevent");
        FileUtils.forceMkdir(eventDir);
        FileUtils.copyFile(
                new File(new File(EVENT_RESOURCES_DIR, "testtestevent"), "TestTestEvent_v1.1.0.avdl"),
                new File(eventDir, "TestTestEvent_v1.avdl"));

        ArrayNode versions = new ArrayNode(factory);
        versions.add(new ObjectNode(factory, Map.of(
                "version", new TextNode("1.1.0"),
                "valueSchema", new TextNode("TestTestEvent_v1.avdl"))));
        JsonNode jsonNode = new ObjectNode(factory, Map.of(
                "versions", versions));

        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .messageTypeName("TestTestEvent")
                .messageTypeDirectory(eventDir)
                .messagingType(MessagingType.EVENT)
                .importClassLoader(new ImportClassLoader(new ImportClassLoader(AVRO_DIR), commonSystemDir, new File("nonexisting")))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode, oldDescriptor);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void importNonExisting(@TempDir File tmpDir) throws IOException {
        File eventDir = new File(tmpDir + "/" + MessageTypeRegistryConstants.EVENT_DIR_NAME, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(eventDir);
        FileUtils.copyFile(new File(new File(EVENT_RESOURCES_DIR, "testtestevent"), "TestTestEvent_v1.1.0.avdl"), new File(eventDir, "TestTestEvent_v1.avdl"));

        ArrayNode versions = new ArrayNode(factory);
        versions.add(new ObjectNode(factory, Map.of(
                "version", new IntNode(1),
                "valueSchema", new TextNode("TestTestEvent_v1.avdl"))));
        JsonNode jsonNode = new ObjectNode(factory, Map.of(
                "versions", versions));

        ValidationContext validationContext = ValidationContext.builder()
                .descriptorFile(new File("test"))
                .messageTypeName("TestTestEvent")
                .messageTypeDirectory(eventDir)
                .messagingType(MessagingType.EVENT)
                .importClassLoader(new ImportClassLoader(AVRO_DIR))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode, oldDescriptor);

        assertFalse(result.isValid(), "Importing a file that does not exist. This should fail");
    }
}
