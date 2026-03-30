package ch.admin.bit.jeap.messaging.avro.pluginIntegration;

import ch.admin.bit.jeap.messaging.avro.plugin.mojo.ProtocolMojo;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@MojoTest
class ProtocolMojoTest extends AbstractAvroMojoTest {

    private static final String BASEDIR = "src/test/resources/sample-protocol-event";

    @AfterEach
    void cleanup() {
        deleteTargetDir(BASEDIR);
    }

    @Test
    @Basedir(BASEDIR)
    @InjectMojo(goal = "protocol")
    void execute(ProtocolMojo myMojo) throws Exception {
        myMojo.execute();
        final List<String> filenames = readAllFiles(new File(BASEDIR, "target/generated-sources"));
        assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }
}
