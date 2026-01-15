package ch.admin.bit.jeap.messaging.avro.pluginIntegration;

import ch.admin.bit.jeap.messaging.avro.plugin.mojo.MessageTypeArtifactsDeployerMojo;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("java:S1874")
public class MessageTypeArtifactsDeployerMojoTest extends AbstractAvroMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Test
    public void execute_noSourcesDirectory_nothingDeployed() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-message-type-artifacts-deploy");
        final File targetDirectory = new File(testDirectory, "target/generated-sources");
        final MessageTypeArtifactsDeployerMojo myMojo = (MessageTypeArtifactsDeployerMojo) mojoRule.lookupConfiguredMojo(testDirectory, "deploy-message-type-artifacts");

        // act
        myMojo.execute();

        // assert
        Assert.assertFalse(Files.exists(Path.of(targetDirectory.getAbsolutePath())));
    }

    @Test
    public void execute_sourcesDirectoryExists_projectsDeployed() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-message-type-artifacts-deploy");
        final File targetDirectory = new File(testDirectory, "target/generated-sources");
        Files.createDirectories(targetDirectory.toPath());
        FileUtils.copyDirectory(Paths.get("src/test/resources/sample-project").toFile(), Paths.get(targetDirectory.getAbsolutePath()).toFile());
        final MessageTypeArtifactsDeployerMojo myMojo = (MessageTypeArtifactsDeployerMojo) mojoRule.lookupConfiguredMojo(testDirectory, "deploy-message-type-artifacts");

        // act
        try {
            myMojo.execute();
        } catch (MojoExecutionException e) {
            // assert
            Assert.assertEquals("Build failed with exitCode 127", e.getMessage());
        }
    }

}
