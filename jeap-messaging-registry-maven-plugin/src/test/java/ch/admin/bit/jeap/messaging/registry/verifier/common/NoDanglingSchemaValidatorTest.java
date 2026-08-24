package ch.admin.bit.jeap.messaging.registry.verifier.common;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NoDanglingSchemaValidatorTest {

    @Test
    void validate_ok(@TempDir File tempDir) throws Exception {
        Path path = tempDir.toPath();
        Files.createFile(path.resolve("ActivZoneEnteredEvent_v1.0.0.avdl"));
        Files.createFile(path.resolve("ActivZoneEnteredEvent_v2.0.0.avdl"));
        Files.createFile(path.resolve("Key_v1.0.0.avdl"));
        JsonNode descriptor = createDescriptor();

        ValidationResult result = NoDanglingSchemaValidator.validate(tempDir, descriptor);

        assertThat(result.isValid())
                .isTrue();
    }

    @Test
    void validate_fail_danglingFile(@TempDir File tempDir) throws Exception {
        Path path = tempDir.toPath();
        Files.createFile(path.resolve("ActivZoneEnteredEvent_v1.0.0.avdl"));
        Files.createFile(path.resolve("ActivZoneEnteredEvent_v2.0.0.avdl"));
        Files.createFile(path.resolve("Key_v1.0.0.avdl"));
        Files.createFile(path.resolve("Dangling_v1.0.0.avdl"));
        Files.createFile(path.resolve("Dangling_v2.0.0.avdl"));
        JsonNode descriptor = createDescriptor();

        ValidationResult result = NoDanglingSchemaValidator.validate(tempDir, descriptor);

        assertThat(result.isValid())
                .isFalse();
        assertThat(result.getErrors())
                .containsOnly(
                        "Schema file Dangling_v1.0.0.avdl is not referenced",
                        "Schema file Dangling_v2.0.0.avdl is not referenced");
    }

    private static JsonNode createDescriptor() throws JacksonException {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        return objectMapper.readTree("""
                        {
                          "versions": [
                            {
                              "version": "1.0.0",
                              "valueSchema": "ActivZoneEnteredEvent_v1.0.0.avdl"
                            },
                            {
                              "version": "2.0.0",
                              "valueSchema": "ActivZoneEnteredEvent_v2.0.0.avdl",
                              "keySchema": "Key_v1.0.0.avdl"
                            }
                          ]
                        }
                        """);
    }
}
