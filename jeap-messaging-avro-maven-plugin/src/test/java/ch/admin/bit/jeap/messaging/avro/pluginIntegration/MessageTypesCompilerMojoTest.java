package ch.admin.bit.jeap.messaging.avro.pluginIntegration;

import ch.admin.bit.jeap.messaging.avro.plugin.mojo.MessageTypesCompilerMojo;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageTypesCompilerMojoTest extends AbstractAvroMojoTest {

    private static final String EXPECTED_EVENT_TYPE_REF = "public static class TypeRef implements ch.admin.bit.jeap.messaging.avro.MessageTypeMetadata {\n" +
                                                          "    public static final String REGISTRY_URL = \"gitUrl\";\n" +
            "    public static final String REGISTRY_BRANCH = \"my-branch\";\n" +
                                                          "    public static final String REGISTRY_COMMIT = \"cafebabe\";\n" +
                                                          "    public static final String COMPATIBILITY_MODE = \"BACKWARD\";\n" +
                                                          "    public static final String MESSAGE_TYPE_VERSION = \"1.1.0\";\n" +
                                                          "    public static final String MESSAGE_TYPE_NAME = \"JmeDeclarationCreatedEvent\";\n" +
                                                          "    public static final String SYSTEM_NAME = \"JME\";\n" +
                                                          "    public static final String TOPIC_ADDITIONAL_TOPIC = \"additional-topic\";\n" +
                                                          "    public static final String TOPIC_JME_MESSAGING_DECLARATION_CREATED = \"jme-messaging-declaration-created\";\n" +
                                                          "    public static final String DEFAULT_TOPIC = \"jme-messaging-declaration-created\";\n" +
                                                          "}";
    private static final String EXPECTED_COMMAND_TYPE_REF = "public static class TypeRef implements ch.admin.bit.jeap.messaging.avro.MessageTypeMetadata {\n" +
                                                            "    public static final String REGISTRY_URL = \"gitUrl\";\n" +
            "    public static final String REGISTRY_BRANCH = \"my-branch\";\n" +
                                                            "    public static final String REGISTRY_COMMIT = \"cafebabe\";\n" +
                                                            "    public static final String COMPATIBILITY_MODE = \"BACKWARD\";\n" +
                                                            "    public static final String MESSAGE_TYPE_VERSION = \"1.0.0\";\n" +
                                                            "    public static final String MESSAGE_TYPE_NAME = \"JmeCreateDeclarationCommand\";\n" +
                                                            "    public static final String SYSTEM_NAME = \"JME\";\n" +
                                                            "    public static final String TOPIC_ADDITIONAL_TOPIC = \"additional-topic\";\n" +
                                                            "    public static final String TOPIC_JME_MESSAGING_CREATE_DECLARATION = \"jme-messaging-create-declaration\";\n" +
                                                            "    public static final String DEFAULT_TOPIC = \"jme-messaging-create-declaration\";\n" +
                                                            "}";

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Test
    public void execute_generateAllMessageTypes_allMessageTypesGenerated() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-message-type-registry");
        //with the real plugin these 2 definitions are available in the classpath
        Files.createDirectories(Paths.get(testDirectory.getAbsolutePath(), "target", "classes"));
        Files.copy(Paths.get(testDirectory.getAbsolutePath(), "MessagingBaseTypes.avdl"), Paths.get(testDirectory.getAbsolutePath(), "target", "classes", "MessagingBaseTypes.avdl"));
        Files.copy(Paths.get(testDirectory.getAbsolutePath(), "DomainEventBaseTypes.avdl"), Paths.get(testDirectory.getAbsolutePath(), "target", "classes", "DomainEventBaseTypes.avdl"));

        FileUtils.copyDirectory(Paths.get(Paths.get("").toAbsolutePath().getParent().toString(), ".git").toFile(), Paths.get(testDirectory.getAbsolutePath(), ".git").toFile());

        final MessageTypesCompilerMojo myMojo = (MessageTypesCompilerMojo) mojoRule.lookupConfiguredMojo(testDirectory, "compile-message-types");

        myMojo.setGenerateAllMessageTypes(true);
        myMojo.setCurrentBranch("my-branch");
        myMojo.setCommitId("cafebabe");
        myMojo.setGitUrl("gitUrl");
        myMojo.setFetchTags(false); // not a valid git url provided, so we cannot fetch tags
        myMojo.setGroupIdPrefix("ch.bit.admin.test");

        // act
        myMojo.execute();

        // assert
        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        Assert.assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
        assertSystemCommonFilesRemoved(filenames);

        assertEquals(8, filenames.stream().filter(f -> f.endsWith("/pom.xml")).count());

        assertContentOfCreatedSourceDirectory(testDirectory, "target/generated-sources/jme/_common/src/main/java/ch/admin/bit/jme/declaration", 4);

        assertContentOfCreatedSourceDirectory(testDirectory, "target/generated-sources/jme/JmeCreateDeclarationCommand/1.0.0/src/main/java/ch/admin/bit/jme/declaration", 4);
        assertContentOfCreatedSourceDirectory(testDirectory, "target/generated-sources/jme/JmeCreateDeclarationCommand/1.1.0/src/main/java/ch/admin/bit/jme/declaration", 6);

        assertContentOfCreatedSourceDirectory(testDirectory, "target/generated-sources/jme/JmeDeclarationCreatedEvent/1.1.0/src/main/java/ch/admin/bit/jme/declaration", 5);
        assertContentOfCreatedSourceDirectory(testDirectory, "target/generated-sources/jme/JmeDeclarationCreatedEvent/1.4.0/src/main/java/ch/admin/bit/jme/declaration", 4);

        assertContentOfCreatedSourceDirectory(testDirectory, "target/generated-sources/wvs/WvsCreateDeclarationCommand/1.0.0/src/main/java/ch/admin/bit/wvs/declaration", 4);

        String commandContent = Files.readString(new File(testDirectory, "target/generated-sources/jme/JmeCreateDeclarationCommand/1.0.0/src/main/java/ch/admin/bit/jme/declaration/JmeCreateDeclarationCommand.java").toPath());
        String eventContent = Files.readString(new File(testDirectory, "target/generated-sources/jme/JmeDeclarationCreatedEvent/1.1.0/src/main/java/ch/admin/bit/jme/declaration/JmeDeclarationCreatedEvent.java").toPath());
        System.out.println(commandContent);
        System.out.println(eventContent);
        assertThat(commandContent)
                .containsIgnoringWhitespaces(EXPECTED_COMMAND_TYPE_REF);
        assertThat(eventContent)
                .containsIgnoringWhitespaces(EXPECTED_EVENT_TYPE_REF);

        assertFileContains(testDirectory, "target/generated-sources/jme/JmeCreateDeclarationCommand/1.0.0/pom.xml", "<classifier>1.0.0-my-branch-SNAPSHOT</classifier>");
        assertFileContains(testDirectory, "target/generated-sources/jme/JmeDeclarationCreatedEvent/1.4.0/pom.xml", "<classifier>1.4.0-my-branch-SNAPSHOT</classifier>");
    }

    @Test
    public void execute_generateAllMessageTypes_customPomTemplate() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-message-type-registry-custom-pom");
        //with the real plugin these 2 definitions are available in the classpath
        Files.createDirectories(Paths.get(testDirectory.getAbsolutePath(), "target", "classes"));
        Files.copy(Paths.get(testDirectory.getAbsolutePath(), "MessagingBaseTypes.avdl"), Paths.get(testDirectory.getAbsolutePath(), "target", "classes", "MessagingBaseTypes.avdl"));
        Files.copy(Paths.get(testDirectory.getAbsolutePath(), "DomainEventBaseTypes.avdl"), Paths.get(testDirectory.getAbsolutePath(), "target", "classes", "DomainEventBaseTypes.avdl"));

        FileUtils.copyDirectory(Paths.get(Paths.get("").toAbsolutePath().getParent().toString(), ".git").toFile(), Paths.get(testDirectory.getAbsolutePath(), ".git").toFile());

        MessageTypesCompilerMojo myMojo = (MessageTypesCompilerMojo) mojoRule.lookupConfiguredMojo(testDirectory, "compile-message-types");

        myMojo.setGenerateAllMessageTypes(true);
        myMojo.setCurrentBranch("my-branch");
        myMojo.setCommitId("cafebabe");
        myMojo.setGitUrl("gitUrl");
        myMojo.setFetchTags(false); // not a valid git url provided, so we cannot fetch tags
        myMojo.setGroupIdPrefix("ch.bit.admin.test");
        myMojo.setPomTemplateFile(new File(testDirectory, "messagetype-template.pom.xml"));

        // act
        myMojo.execute();

        // assert
        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        Assert.assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);

        assertEquals(1, filenames.stream().filter(f -> f.endsWith("/pom.xml")).count());
        assertContentOfCreatedSourceDirectory(testDirectory,
                "target/generated-sources/jme/JmeDeclarationCreatedEvent/1.1.0/src/main/java/ch/admin/bit/jme/declaration", 4);
        assertFileContains(testDirectory,
                "target/generated-sources/jme/JmeDeclarationCreatedEvent/1.1.0/pom.xml",
                "<version>1.1.0-my-branch-SNAPSHOT</version>");
        assertFileContains(testDirectory,
                "target/generated-sources/jme/JmeDeclarationCreatedEvent/1.1.0/pom.xml",
                "<custom.property>value</custom.property>");
    }

    @Test
    public void execute_generateAllMessageTypes_correctClassifierForMasterBranch() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-message-type-registry");
        //with the real plugin these 2 definitions are available in the classpath
        Files.createDirectories(Paths.get(testDirectory.getAbsolutePath(), "target", "classes"));
        Files.copy(Paths.get(testDirectory.getAbsolutePath(), "MessagingBaseTypes.avdl"), Paths.get(testDirectory.getAbsolutePath(), "target", "classes", "MessagingBaseTypes.avdl"));
        Files.copy(Paths.get(testDirectory.getAbsolutePath(), "DomainEventBaseTypes.avdl"), Paths.get(testDirectory.getAbsolutePath(), "target", "classes", "DomainEventBaseTypes.avdl"));

        FileUtils.copyDirectory(Paths.get(Paths.get("").toAbsolutePath().getParent().toString(), ".git").toFile(), Paths.get(testDirectory.getAbsolutePath(), ".git").toFile());

        final MessageTypesCompilerMojo myMojo = (MessageTypesCompilerMojo) mojoRule.lookupConfiguredMojo(testDirectory, "compile-message-types");

        myMojo.setGenerateAllMessageTypes(true);
        myMojo.setCurrentBranch("master");
        myMojo.setCommitId("cafebabe");
        myMojo.setGitUrl("gitUrl");
        myMojo.setFetchTags(false); // not a valid git url provided, so we cannot fetch tags
        myMojo.setGroupIdPrefix("ch.bit.admin.test");

        // act
        myMojo.execute();

        // assert
        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        Assert.assertFalse(filenames.isEmpty());
        assertFileContains(testDirectory, "target/generated-sources/jme/JmeCreateDeclarationCommand/1.0.0/pom.xml", "<classifier>1.0.0</classifier>");
        assertFileContains(testDirectory, "target/generated-sources/jme/JmeDeclarationCreatedEvent/1.4.0/pom.xml", "<classifier>1.4.0</classifier>");
    }

    private void assertSystemCommonFilesRemoved(List<String> filenames) {
        List<String> classes = filenames.stream()
                .filter(this::isSystemCommonFile)
                .toList();
        assertEquals(4, classes.size());
    }

    private boolean isSystemCommonFile(String filename) {
        return !filename.contains("delombok") && (filename.endsWith("BeanReference.java") || filename.endsWith("BeanReferenceMessageKey.java") || filename.endsWith("BeanReferenceMessageKeyProtocol.java") || filename.endsWith("BeanReferenceProtocol.java"));
    }

    private void assertContentOfCreatedSourceDirectory(File testDirectory, String child, int count) {
        final File directory = new File(testDirectory, child);
        assertTrue(directory.exists());
        assertEquals(count, Objects.requireNonNull(directory.listFiles()).length);
    }

    private void assertFileContains(File baseDirectory, String filename, String text) throws IOException {
        String fileContent = Files.readString(new File(baseDirectory, filename).toPath());
        assertThat(fileContent)
                .contains(text);
    }
}
