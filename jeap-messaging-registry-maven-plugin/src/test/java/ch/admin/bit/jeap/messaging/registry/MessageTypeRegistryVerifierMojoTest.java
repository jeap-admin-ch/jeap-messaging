package ch.admin.bit.jeap.messaging.registry;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SuppressWarnings("java:S1874")
class MessageTypeRegistryVerifierMojoTest extends AbstractMojoTestCase {
    private final static File RESOURCES_DIR = new File("src/test/resources/");

    @BeforeEach
    void setupTestDir() throws Exception {
        setUp();
    }

    @Test
    void invalid(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "dirMissing");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertThatThrownBy(target::execute)
                .hasMessageContaining("descriptor' does not exist");
    }

    @Test
    void contracts(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "contracts");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertThatThrownBy(target::execute)
                .hasMessageContaining("object instance has properties which are not allowed by the schema: [\"contracts\"]");
    }

    @Test
    void valid(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "valid");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertDoesNotThrow(target::execute);
    }

    @Test
    void messageTypeNameMissingInSchema(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "messageTypeNameMissingInSchema");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertThatThrownBy(target::execute)
                .hasMessageContaining("Message type AutorisaziunPermitValidationDeclinedEvent not found in any schema for version 2.0.0");
    }

    @Test
    void danglingFile(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "danglingFile");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertThatThrownBy(target::execute)
                .hasMessageContaining("Schema file TestTestEvent_v999.avdl is not referenced")
                .hasMessageContaining("Schema file TestTestCommand_v999.avdl is not referenced");
    }

    @Test
    void validMultipleTopics(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "validMultipleTopics");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertDoesNotThrow(target::execute);
    }

    @Test
    void validSystemDefinedGlobally(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "validSystemDefinedGlobally");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertDoesNotThrow(target::execute);
    }

    @Test
    void commonData(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "commonData");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertDoesNotThrow(target::execute);
    }

    @Test
    void validCommandMinorVersionUpdate(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "validCommandMinorVersionUpdate");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertDoesNotThrow(target::execute);
    }

    /**
     * Those tests are to ensure that using "publishingSystem" is still supported. They can be deleted when this
     * does not need to be the case any more
     */
    @Test
    void publishingSystem(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "publishingSystem");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertDoesNotThrow(target::execute);
    }

    @Test
    void unusedImport(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "unusedImport");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertThatThrownBy(target::execute)
                .hasMessageContaining("Unused imports")
                .hasMessageContaining("ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestEnum.avdl");
    }

    private Mojo open(File basedir) throws Exception {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory(basedir);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession(new DefaultRepositorySystemSession());

        File pom = new File(basedir, "pom.xml");
        MavenProject project = getContainer().lookup(ProjectBuilder.class).build(pom, configuration).getProject();
        MavenSession session = newMavenSession(project);
        MojoExecution execution = newMojoExecution("registry");
        return lookupConfiguredMojo(session, execution);
    }
}
