package ch.admin.bit.jeap.messaging.avro.pluginIntegration;

import ch.admin.bit.jeap.messaging.avro.plugin.mojo.MessageTypesCompilerMojo;
import org.apache.commons.io.FileUtils;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@MojoTest
class MessageTypesCompilerMojoTest extends AbstractAvroMojoTest {

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

    @Inject
    private MavenProject project;

    private File setupTestDirectory(Path tempDir, String resourcePath) throws Exception {
        File testDirectory = syncToTempDirectory(resourcePath, tempDir);

        Files.createDirectories(Paths.get(testDirectory.getAbsolutePath(), "target", "classes"));
        Files.copy(Paths.get(testDirectory.getAbsolutePath(), "MessagingBaseTypes.avdl"), Paths.get(testDirectory.getAbsolutePath(), "target", "classes", "MessagingBaseTypes.avdl"));
        Files.copy(Paths.get(testDirectory.getAbsolutePath(), "DomainEventBaseTypes.avdl"), Paths.get(testDirectory.getAbsolutePath(), "target", "classes", "DomainEventBaseTypes.avdl"));

        FileUtils.copyDirectory(Paths.get(Paths.get("").toAbsolutePath().getParent().toString(), ".git").toFile(), Paths.get(testDirectory.getAbsolutePath(), ".git").toFile());

        return testDirectory;
    }

    /**
     * Point the project and mojo at the temp directory so paths resolve correctly.
     * The project is already the same instance injected into the mojo by @InjectMojo,
     * so updating its basedir also affects the mojo's runtime behavior.
     */
    private void pointToTempDir(MessageTypesCompilerMojo myMojo, File testDirectory) throws IllegalAccessException {
        setVariableValueToObject(project, "basedir", testDirectory);
        project.getBuild().setDirectory(new File(testDirectory, "target").getAbsolutePath());
        project.getBuild().setOutputDirectory(new File(testDirectory, "target/classes").getAbsolutePath());
        setVariableValueToObject(myMojo, "sourceDirectory", new File(testDirectory, "descriptor"));
        setVariableValueToObject(myMojo, "outputDirectory", new File(testDirectory, "target/generated-sources"));
    }

    @Test
    @InjectMojo(goal = "compile-message-types", pom = "src/test/resources/sample-message-type-registry/pom.xml")
    void execute_generateAllMessageTypes_allMessageTypesGenerated(MessageTypesCompilerMojo myMojo, @TempDir Path tempDir) throws Exception {
        final File testDirectory = setupTestDirectory(tempDir, "src/test/resources/sample-message-type-registry");
        pointToTempDir(myMojo, testDirectory);

        myMojo.setGenerateAllMessageTypes(true);
        myMojo.setCurrentBranch("my-branch");
        myMojo.setCommitId("cafebabe");
        myMojo.setGitUrl("gitUrl");
        myMojo.setFetchTags(false);
        myMojo.setGroupIdPrefix("ch.bit.admin.test");

        myMojo.execute();

        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        assertFalse(filenames.isEmpty());
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
        assertThat(commandContent).containsIgnoringWhitespaces(EXPECTED_COMMAND_TYPE_REF);
        assertThat(eventContent).containsIgnoringWhitespaces(EXPECTED_EVENT_TYPE_REF);

        assertFileContains(testDirectory, "target/generated-sources/jme/JmeCreateDeclarationCommand/1.0.0/pom.xml", "<classifier>1.0.0-my-branch-SNAPSHOT</classifier>");
        assertFileContains(testDirectory, "target/generated-sources/jme/JmeDeclarationCreatedEvent/1.4.0/pom.xml", "<classifier>1.4.0-my-branch-SNAPSHOT</classifier>");
    }

    @Test
    @InjectMojo(goal = "compile-message-types", pom = "src/test/resources/sample-message-type-registry-custom-pom/pom.xml")
    void execute_generateAllMessageTypes_customPomTemplate(MessageTypesCompilerMojo myMojo, @TempDir Path tempDir) throws Exception {
        final File testDirectory = setupTestDirectory(tempDir, "src/test/resources/sample-message-type-registry-custom-pom");
        pointToTempDir(myMojo, testDirectory);

        myMojo.setGenerateAllMessageTypes(true);
        myMojo.setCurrentBranch("my-branch");
        myMojo.setCommitId("cafebabe");
        myMojo.setGitUrl("gitUrl");
        myMojo.setFetchTags(false);
        myMojo.setGroupIdPrefix("ch.bit.admin.test");
        myMojo.setPomTemplateFile(new File(testDirectory, "messagetype-template.pom.xml"));

        myMojo.execute();

        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        assertFalse(filenames.isEmpty());
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
    @InjectMojo(goal = "compile-message-types", pom = "src/test/resources/sample-message-type-registry/pom.xml")
    void execute_generateAllMessageTypes_correctClassifierForMasterBranch(MessageTypesCompilerMojo myMojo, @TempDir Path tempDir) throws Exception {
        final File testDirectory = setupTestDirectory(tempDir, "src/test/resources/sample-message-type-registry");
        pointToTempDir(myMojo, testDirectory);

        myMojo.setGenerateAllMessageTypes(true);
        myMojo.setCurrentBranch("master");
        myMojo.setCommitId("cafebabe");
        myMojo.setGitUrl("gitUrl");
        myMojo.setFetchTags(false);
        myMojo.setGroupIdPrefix("ch.bit.admin.test");

        myMojo.execute();

        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        assertFalse(filenames.isEmpty());
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
        assertThat(fileContent).contains(text);
    }
}
