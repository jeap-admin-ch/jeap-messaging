package ch.admin.bit.jeap.messaging.registry;

import org.apache.commons.io.FileUtils;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.inject.Inject;
import java.io.File;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@MojoTest
class MessageTypeRegistryVerifierMojoTest {
    private static final File RESOURCES_DIR = new File("src/test/resources/");

    @Inject
    private MavenProject project;

    private void configureMojo(Mojo mojo, File tmpDir) throws Exception {
        setVariableValueToObject(project, "basedir", tmpDir);
        project.getBuild().setDirectory(new File(tmpDir, "target").getAbsolutePath());
        project.getBuild().setOutputDirectory(new File(tmpDir, "target/classes").getAbsolutePath());
        setVariableValueToObject(mojo, "descriptorDirectory", new File(tmpDir, "descriptor"));
        setVariableValueToObject(mojo, "project", project);
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void invalid(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "dirMissing"), tmpDir);
        configureMojo(target, tmpDir);
        assertThatThrownBy(target::execute)
                .hasMessageContaining("descriptor' does not exist");
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void contracts(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "contracts"), tmpDir);
        configureMojo(target, tmpDir);
        assertThatThrownBy(target::execute)
                .hasMessageContaining("object instance has properties which are not allowed by the schema: [\"contracts\"]");
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void valid(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "valid"), tmpDir);
        configureMojo(target, tmpDir);
        assertDoesNotThrow(target::execute);
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void messageTypeNameMissingInSchema(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "messageTypeNameMissingInSchema"), tmpDir);
        configureMojo(target, tmpDir);
        assertThatThrownBy(target::execute)
                .hasMessageContaining("Message type AutorisaziunPermitValidationDeclinedEvent not found in any schema for version 2.0.0");
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void danglingFile(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "danglingFile"), tmpDir);
        configureMojo(target, tmpDir);
        assertThatThrownBy(target::execute)
                .hasMessageContaining("Schema file TestTestEvent_v999.avdl is not referenced")
                .hasMessageContaining("Schema file TestTestCommand_v999.avdl is not referenced");
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void validMultipleTopics(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "validMultipleTopics"), tmpDir);
        configureMojo(target, tmpDir);
        assertDoesNotThrow(target::execute);
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void validSystemDefinedGlobally(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "validSystemDefinedGlobally"), tmpDir);
        configureMojo(target, tmpDir);
        assertDoesNotThrow(target::execute);
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void commonData(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "commonData"), tmpDir);
        configureMojo(target, tmpDir);
        assertDoesNotThrow(target::execute);
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void validCommandMinorVersionUpdate(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "validCommandMinorVersionUpdate"), tmpDir);
        configureMojo(target, tmpDir);
        assertDoesNotThrow(target::execute);
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void publishingSystem(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "publishingSystem"), tmpDir);
        configureMojo(target, tmpDir);
        assertDoesNotThrow(target::execute);
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void unusedImport(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "unusedImport"), tmpDir);
        configureMojo(target, tmpDir);
        assertThatThrownBy(target::execute)
                .hasMessageContaining("Unused imports")
                .hasMessageContaining("ch.admin.bit.jeap.domainevent.registry.verifier.testevent.TestEnum.avdl");
    }

}
