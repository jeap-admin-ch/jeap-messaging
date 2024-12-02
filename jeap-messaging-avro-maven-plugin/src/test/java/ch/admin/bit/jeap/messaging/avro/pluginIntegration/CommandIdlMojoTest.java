package ch.admin.bit.jeap.messaging.avro.pluginIntegration;

import ch.admin.bit.jeap.messaging.avro.plugin.mojo.IDLProtocolMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.List;

public class CommandIdlMojoTest extends AbstractAvroMojoTest {
    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void execute() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-idl-command");
        final IDLProtocolMojo myMojo = (IDLProtocolMojo) mojoRule.lookupConfiguredMojo(testDirectory, "idl");
        // act
        myMojo.execute();
        // assert
        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        Assert.assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }

    @Test
    public void execute_optionalArrayReferences() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-idl-command-optional-array-references");
        final IDLProtocolMojo myMojo = (IDLProtocolMojo) mojoRule.lookupConfiguredMojo(testDirectory, "idl");
        // act
        myMojo.execute();
        // assert
        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        Assert.assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }

    @Test(expected = MojoExecutionException.class)
    public void execute_optionalArrayReferencesWithWrongPostfix() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-idl-command-optional-array-references-wrong-postfix");
        final IDLProtocolMojo myMojo = (IDLProtocolMojo) mojoRule.lookupConfiguredMojo(testDirectory, "idl");
        // act
        myMojo.execute();
    }

    @Test
    public void execute_wrongReferenceShouldFail() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-idl-command-wrong-references-type");
        final IDLProtocolMojo myMojo = (IDLProtocolMojo) mojoRule.lookupConfiguredMojo(testDirectory, "idl");
        exceptionRule.expect(MojoExecutionException.class);
        exceptionRule.expectMessage("At least one avro schema is not a valid message");
        // act
        myMojo.execute();
    }

}
