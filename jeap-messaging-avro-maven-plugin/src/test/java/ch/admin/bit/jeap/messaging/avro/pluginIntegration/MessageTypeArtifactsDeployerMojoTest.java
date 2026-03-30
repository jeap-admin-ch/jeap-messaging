package ch.admin.bit.jeap.messaging.avro.pluginIntegration;

import ch.admin.bit.jeap.messaging.avro.plugin.mojo.MessageTypeArtifactsDeployerMojo;
import org.apache.commons.io.FileUtils;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@MojoTest
class MessageTypeArtifactsDeployerMojoTest extends AbstractAvroMojoTest {

    private static final String BASEDIR = "src/test/resources/sample-message-type-artifacts-deploy";

    @AfterEach
    void cleanup() {
        deleteTargetDir(BASEDIR);
    }

    @Test
    @Basedir(BASEDIR)
    @InjectMojo(goal = "deploy-message-type-artifacts")
    void execute_noSourcesDirectory_nothingDeployed(MessageTypeArtifactsDeployerMojo myMojo) throws Exception {
        myMojo.execute();
        assertFalse(Files.exists(Path.of(BASEDIR, "target/generated-sources")));
    }

    @Test
    @Basedir(BASEDIR)
    @InjectMojo(goal = "deploy-message-type-artifacts")
    void execute_sourcesDirectoryExists_projectsDeployed(MessageTypeArtifactsDeployerMojo myMojo) throws Exception {
        File targetDirectory = new File(BASEDIR, "target/generated-sources");
        Files.createDirectories(targetDirectory.toPath());
        FileUtils.copyDirectory(Paths.get("src/test/resources/sample-project").toFile(), targetDirectory);

        try {
            myMojo.execute();
        } catch (MojoExecutionException e) {
            assertEquals("Build failed with exitCode 127", e.getMessage());
        }
    }
}
