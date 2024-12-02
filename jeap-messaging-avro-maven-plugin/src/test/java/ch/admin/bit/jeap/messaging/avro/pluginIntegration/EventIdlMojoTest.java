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

public class EventIdlMojoTest extends AbstractAvroMojoTest {
    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void execute() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-idl-event");
        final IDLProtocolMojo myMojo = (IDLProtocolMojo) mojoRule.lookupConfiguredMojo(testDirectory, "idl");
        // act
        myMojo.execute();
        // assert
        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        Assert.assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }

    @Test
    public void execute_optionalEventFields() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-idl-event-optional-payload");
        final IDLProtocolMojo myMojo = (IDLProtocolMojo) mojoRule.lookupConfiguredMojo(testDirectory, "idl");
        // act
        myMojo.execute();
        // assert
        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        Assert.assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }

    @Test
    public void execute_optionalEventFieldsReferences() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-idl-event-optional-references");
        final IDLProtocolMojo myMojo = (IDLProtocolMojo) mojoRule.lookupConfiguredMojo(testDirectory, "idl");
        // act
        myMojo.execute();
        // assert
        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        Assert.assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }

    @Test
    public void execute_optionalComplexReferencesEvent() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-idl-event-complex-references");
        final IDLProtocolMojo myMojo = (IDLProtocolMojo) mojoRule.lookupConfiguredMojo(testDirectory, "idl");
        // act
        myMojo.execute();
        // assert
        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        Assert.assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }

    @Test
    public void execute_wrongReferencesRecordOutsideStructureFromEventShouldNotBeValidated() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-idl-event-references-outside-structure");
        final IDLProtocolMojo myMojo = (IDLProtocolMojo) mojoRule.lookupConfiguredMojo(testDirectory, "idl");
        // act
        myMojo.execute();
        // assert
        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        Assert.assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }

    @Test
    public void execute_wrongReferenceShouldFail() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-idl-event-array-references-wrong-reference-structure");
        final IDLProtocolMojo myMojo = (IDLProtocolMojo) mojoRule.lookupConfiguredMojo(testDirectory, "idl");
        exceptionRule.expect(MojoExecutionException.class);
        exceptionRule.expectMessage("At least one avro schema is not a valid message");
        // act
        myMojo.execute();
    }
}
