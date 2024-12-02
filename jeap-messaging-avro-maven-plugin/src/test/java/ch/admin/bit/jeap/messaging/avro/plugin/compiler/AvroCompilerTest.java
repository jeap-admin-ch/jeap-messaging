package ch.admin.bit.jeap.messaging.avro.plugin.compiler;

import ch.admin.bit.jeap.messaging.avro.plugin.interfaces.InterfaceTool;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.metadata.MessageTypeMetadata;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvroCompilerTest {

    private Path tempOutputDir;

    private static AvroCompiler generateCompiler(Path outputDirectory) {
        return AvroCompiler.builder()
                .sourceEncoding(Charset.defaultCharset().name())
                .outputDirectory(outputDirectory.toFile())
                .additionalTool(new InterfaceTool())
                .build();
    }

    @BeforeEach
    void init() throws IOException {
        tempOutputDir = Files.createTempDirectory("AvroCompilerTestOutputDirectory");
    }

    @AfterEach
    void cleanUp() throws IOException {
        FileUtils.deleteDirectory(tempOutputDir.toFile());
    }

    @Test
    void schema() throws IOException {
        File src = new File("src/test/resources/unittest/validEvent.avsc");
        Schema schema = new Schema.Parser().parse(src);
        AvroCompiler target = generateCompiler(tempOutputDir);

        target.compileSchema(schema, src);

        // Make sure at least some output has been created (there will be additional files created, too).
        assertTrue(isNotEmpty(tempOutputDir.resolve("ch/admin/bit/jeap/domainevent/avro/event/schema")));
    }

    @Test
    void protocol() throws IOException {
        File src = new File("src/test/resources/unittest/validProtocol.avpr");
        Protocol protocol = Protocol.parse(src);
        AvroCompiler target = generateCompiler(tempOutputDir);

        target.compileProtocol(protocol, src);

        // Make sure at least some output has been created (there will be additional files created in other directories, too).
        Path outputPath = tempOutputDir.resolve("ch/admin/bit/jeap/domainevent/avro/event/protocol");
        assertTrue(isNotEmpty(outputPath));

        String eventContent = Files.readString(outputPath.resolve("AvroProtocolTestEvent.java"));
        assertThat(eventContent)
                .doesNotContain("MESSAGE_TYPE_VERSION$")
                .doesNotContain("public static class TypeRef");
    }

    @Test
    void protocol_withMessageTypeVersion() throws IOException {
        File src = new File("src/test/resources/unittest/validProtocol.avpr");
        Protocol protocol = Protocol.parse(src);
        AvroCompiler target = generateCompiler(tempOutputDir).toBuilder()
                .additionalTool(createMessageTypeMetadata())
                .build();

        target.compileProtocol(protocol, src);

        // Make sure at least some output has been created (there will be additional files created in other directories, too).
        Path outputPath = tempOutputDir.resolve("ch/admin/bit/jeap/domainevent/avro/event/protocol");
        assertTrue(isNotEmpty(outputPath));

        String eventContent = Files.readString(outputPath.resolve("AvroProtocolTestEvent.java"));
        assertThat(eventContent).contains("public static final String MESSAGE_TYPE_VERSION$ = \"1.0.2\";");
    }

    @Test
    void protocol_withMessageTypeRef() throws IOException {
        String expectedTypeRef = "public static class TypeRef implements ch.admin.bit.jeap.messaging.avro.MessageTypeMetadata {\n" +
                "    public static final String REGISTRY_URL = \"https://registry\";\n" +
                "    public static final String REGISTRY_BRANCH = \"test\";\n" +
                "    public static final String REGISTRY_COMMIT = \"cafebabe\";\n" +
                "    public static final String COMPATIBILITY_MODE = \"BACKWARD\";\n" +
                "    public static final String MESSAGE_TYPE_VERSION = \"1.0.2\";\n" +
                "    public static final String MESSAGE_TYPE_NAME = \"AvroProtocolTestEvent\";\n" +
                "    public static final String SYSTEM_NAME = \"SYS\";\n" +
                "    public static final String MY_TOPIC = \"my-topic\";\n" +
                "    public static final String MY_TOPIC_2 = \"my-topic-2\";\n" +
                "    public static final String DEFAULT_TOPIC = \"default-topic\";\n" +
                "}";
        File src = new File("src/test/resources/unittest/validProtocol.avpr");
        Protocol protocol = Protocol.parse(src);
        AvroCompiler target = generateCompiler(tempOutputDir).toBuilder()
                .additionalTool(createMessageTypeMetadata())
                .build();

        target.compileProtocol(protocol, src);

        // Make sure at least some output has been created (there will be additional files created in other directories, too).
        Path outputPath = tempOutputDir.resolve("ch/admin/bit/jeap/domainevent/avro/event/protocol");
        assertTrue(isNotEmpty(outputPath));

        String eventContent = Files.readString(outputPath.resolve("AvroProtocolTestEvent.java"));
        assertThat(eventContent)
                .contains("public static class TypeRef")
                .contains(expectedTypeRef);

        String testRefContent = Files.readString(outputPath.resolve("AvroProtocolTestReferences.java"));
        String testProtoContent = Files.readString(outputPath.resolve("AvroProtocolTestProtocol.java"));
        String testPayloadContent = Files.readString(outputPath.resolve("AvroProtocolTestPayload.java"));
        assertThat(testRefContent).doesNotContain("public static class TypeRef");
        assertThat(testProtoContent).doesNotContain("public static class TypeRef");
        assertThat(testPayloadContent).doesNotContain("public static class TypeRef");
    }

    private boolean isNotEmpty(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            files.forEach(System.out::println);
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.findAny().isPresent();
        }
    }

    private static MessageTypeMetadata createMessageTypeMetadata() {
        return MessageTypeMetadata.builder()
                .topicNamesByConstantName(Map.of(
                        "MY_TOPIC", "my-topic",
                        "MY_TOPIC_2", "my-topic-2"))
                .defaultTopic("default-topic")
                .compatibilityMode("BACKWARD")
                .messageTypeName("AvroProtocolTestEvent")
                .messageTypeVersion("1.0.2")
                .registryCommit("cafebabe")
                .registryBranch("test")
                .systemName("SYS")
                .registryUrl("https://registry")
                .build();
    }
}
