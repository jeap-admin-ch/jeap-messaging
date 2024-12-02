package ch.admin.bit.jeap.messaging.registry.exporter;

import lombok.SneakyThrows;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.util.Objects;

class MessageTypeRegistryExporterMojoTest extends AbstractMojoTestCase {
    private final static File RESOURCES_DIR = new File("src/test/resources/");

    @BeforeEach
    void setupTestDir() throws Exception {
        setUp();
    }

    @Test
    void valid(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "valid");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        target.execute();

        assertEquals(new File(tmpDir, "expectedResults"), new File(tmpDir, "target/avro"));
    }

    @Test
    void commonData(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "commonData");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        target.execute();

        assertEquals(new File(tmpDir, "expectedResults"), new File(tmpDir, "target/avro"));
    }

    @SneakyThrows
    private void assertEquals(File expectedResult, File actualResult) {
        Assertions.assertTrue(actualResult.exists(), "Dir " + actualResult + " has not been created");
        for (String system : Objects.requireNonNull(expectedResult.list())) {
            File actualSystemDir = new File(actualResult, system);
            Assertions.assertTrue(actualSystemDir.exists(), "Dir " + actualSystemDir + " has not been created");
            File expectedSystemDir = new File(expectedResult, system);

            for (String systemEventCommandDir : Objects.requireNonNull(expectedSystemDir.list())) {
                File actualEventCommandDir = new File(actualSystemDir, systemEventCommandDir);
                Assertions.assertTrue(actualEventCommandDir.exists(), "Dir " + actualEventCommandDir + " has not been created");
                File expectedEventCommandDir = new File(expectedSystemDir, systemEventCommandDir);

                for (String event : Objects.requireNonNull(expectedEventCommandDir.list())) {
                    File actualEventDir = new File(actualEventCommandDir, event);
                    Assertions.assertTrue(actualEventDir.exists(), "Dir " + actualEventDir + " has not been created");
                    File expectedEventDir = new File(expectedEventCommandDir, event);
                    for (String schema : Objects.requireNonNull(actualEventDir.list())) {
                        File actualSchemaFile = new File(actualEventDir, schema);
                        Assertions.assertTrue(actualSchemaFile.exists(), "File " + actualSchemaFile + " has not been created");
                        File expectedSchemaFile = new File(expectedEventDir, schema);
                        JSONAssert.assertEquals("Expected content of file " + expectedSchemaFile + " but was not", FileUtils.readFileToString(expectedSchemaFile), FileUtils.readFileToString(actualSchemaFile), true);
                    }
                }
            }
        }
    }

    private Mojo open(File basedir) throws Exception {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory(basedir);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession(new DefaultRepositorySystemSession());

        File pom = new File(basedir, "pom.xml");
        MavenProject project = getContainer().lookup(ProjectBuilder.class).build(pom, configuration).getProject();
        MavenSession session = newMavenSession(project);
        MojoExecution execution = newMojoExecution("generateAvdl");
        return lookupConfiguredMojo(session, execution);
    }
}
