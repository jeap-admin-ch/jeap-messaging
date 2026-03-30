package ch.admin.bit.jeap.messaging.avro.pluginIntegration;

import ch.admin.bit.jeap.messaging.avro.plugin.mojo.IDLProtocolMojo;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    @Basedir("src/test/resources/sample-idl-event")
    @InjectMojo(goal = "idl")
    void execute(IDLProtocolMojo myMojo) throws Exception {
        String basedir = "src/test/resources/sample-idl-event";
        trackBasedir(basedir);
        myMojo.execute();
        final List<String> filenames = readAllFiles(new File(basedir, "target/generated-sources"));
        assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }

    @Test
    @Basedir("src/test/resources/sample-idl-event-optional-payload")
    @InjectMojo(goal = "idl")
    void execute_optionalEventFields(IDLProtocolMojo myMojo) throws Exception {
        String basedir = "src/test/resources/sample-idl-event-optional-payload";
        trackBasedir(basedir);
        myMojo.execute();
        final List<String> filenames = readAllFiles(new File(basedir, "target/generated-sources"));
        assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }

    @Test
    @Basedir("src/test/resources/sample-idl-event-optional-references")
    @InjectMojo(goal = "idl")
    void execute_optionalEventFieldsReferences(IDLProtocolMojo myMojo) throws Exception {
        String basedir = "src/test/resources/sample-idl-event-optional-references";
        trackBasedir(basedir);
        myMojo.execute();
        final List<String> filenames = readAllFiles(new File(basedir, "target/generated-sources"));
        assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }

    @Test
    @Basedir("src/test/resources/sample-idl-event-complex-references")
    @InjectMojo(goal = "idl")
    void execute_optionalComplexReferencesEvent(IDLProtocolMojo myMojo) throws Exception {
        String basedir = "src/test/resources/sample-idl-event-complex-references";
        trackBasedir(basedir);
        myMojo.execute();
        final List<String> filenames = readAllFiles(new File(basedir, "target/generated-sources"));
        assertFalse(filenames.isEmpty());
        assertAllCommonEventFilesRemoved(filenames);
    }

    @Test
    @Basedir("src/test/resources/sample-idl-event-references-outside-structure")
    @InjectMojo(goal = "idl")
    void execute_wrongReferencesRecordOutsideStructureFromEventShouldNotBeValidated(IDLProtocolMojo myMojo) throws Exception {
        String basedir = "src/test/resources/sample-idl-event-references-outside-structure";
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
