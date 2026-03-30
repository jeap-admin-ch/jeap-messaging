package ch.admin.bit.jeap.messaging.registry.exporter;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skyscreamer.jsonassert.JSONAssert;

import javax.inject.Inject;
import java.io.File;
import java.util.Objects;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;

@MojoTest
class MessageTypeRegistryExporterMojoTest {
    private static final File RESOURCES_DIR = new File("src/test/resources/");

    @Inject
    private MavenProject project;

    private void configureMojo(Mojo mojo, File tmpDir) throws Exception {
        setVariableValueToObject(project, "basedir", tmpDir);
        project.getBuild().setDirectory(new File(tmpDir, "target").getAbsolutePath());
        project.getBuild().setOutputDirectory(new File(tmpDir, "target/classes").getAbsolutePath());
        setVariableValueToObject(mojo, "descriptorDirectory", new File(tmpDir, "descriptor"));
        setVariableValueToObject(mojo, "targetDir", new File(tmpDir, "target/avro"));
        setVariableValueToObject(mojo, "project", project);
    }

    @Test
    @InjectMojo(goal = "generateAvdl", pom = "src/test/resources/valid/pom.xml")
    void valid(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "valid"), tmpDir);
        configureMojo(target, tmpDir);
        target.execute();

        assertEquals(new File(tmpDir, "expectedResults"), new File(tmpDir, "target/avro"));
    }

    @Test
    @InjectMojo(goal = "generateAvdl", pom = "src/test/resources/valid/pom.xml")
    void commonData(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "commonData"), tmpDir);
        configureMojo(target, tmpDir);
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

}
