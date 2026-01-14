package ch.admin.bit.jeap.messaging.avro.pluginIntegration;

import ch.admin.bit.jeap.messaging.avro.plugin.mojo.SchemaMojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.List;

@SuppressWarnings("java:S1874")
public class SchemaMojoTest extends AbstractAvroMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Test
    public void execute() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-avro-event");
        final SchemaMojo myMojo = (SchemaMojo) mojoRule.lookupConfiguredMojo(testDirectory, "schema");
        // act
        myMojo.execute();
        // assert
        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        Assert.assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }
}
