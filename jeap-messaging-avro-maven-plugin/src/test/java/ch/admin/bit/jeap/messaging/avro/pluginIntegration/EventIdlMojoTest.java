package ch.admin.bit.jeap.messaging.avro.pluginIntegration;

import ch.admin.bit.jeap.messaging.avro.plugin.mojo.IDLProtocolMojo;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MojoTest
class EventIdlMojoTest extends AbstractAvroMojoTest {

    private final List<String> usedBasedirs = new ArrayList<>();

    @AfterEach
    void cleanup() {
        usedBasedirs.forEach(this::deleteTargetDir);
        usedBasedirs.clear();
    }

    private void trackBasedir(String basedir) {
        usedBasedirs.add(basedir);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "src/test/resources/sample-idl-event",
            "src/test/resources/sample-idl-event-optional-payload",
            "src/test/resources/sample-idl-event-optional-references",
            "src/test/resources/sample-idl-event-complex-references",
            "src/test/resources/sample-idl-event-references-outside-structure"
    })
    @Basedir("src/test/resources/sample-idl-event")
    @InjectMojo(goal = "idl")
    void execute(String basedir, IDLProtocolMojo myMojo) throws Exception {
        setVariableValueToObject(myMojo, "sourceDirectory", new File(basedir, "avro"));
        setVariableValueToObject(myMojo, "outputDirectory", new File(basedir, "target/generated-sources"));
        trackBasedir(basedir);
        myMojo.execute();
        final List<String> filenames = readAllFiles(new File(basedir, "target/generated-sources"));
        assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }

    @Test
    @Basedir("src/test/resources/sample-idl-event-array-references-wrong-reference-structure")
    @InjectMojo(goal = "idl")
    void execute_wrongReferenceShouldFail(IDLProtocolMojo myMojo) {
        assertThrows(MojoExecutionException.class, myMojo::execute,
                "At least one avro schema is not a valid message");
    }
}
