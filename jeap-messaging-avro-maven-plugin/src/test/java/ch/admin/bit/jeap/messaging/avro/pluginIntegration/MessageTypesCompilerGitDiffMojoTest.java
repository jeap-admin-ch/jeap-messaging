package ch.admin.bit.jeap.messaging.avro.pluginIntegration;

import ch.admin.bit.jeap.messaging.avro.plugin.mojo.MessageTypesCompilerMojo;
import ch.admin.bit.jeap.messaging.avro.pluginIntegration.repo.TestRegistryRepo;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MojoTest
@ExtendWith(SystemStubsExtension.class)
class MessageTypesCompilerGitDiffMojoTest extends AbstractAvroMojoTest {

    private TestRegistryRepo testRepo;


    @BeforeEach
    void createTestRepo() throws Exception {
        testRepo = TestRegistryRepo.testRepoWithThreeCommitsAddingMessageTypeV1AndV2AndCommand();
        Path testDirectory = testRepo.repoDir();

        Path targetClasses = testDirectory.resolve("target/classes");
        Files.createDirectories(targetClasses);
        Files.copy(testDirectory.resolve("MessagingBaseTypes.avdl"), targetClasses.resolve("MessagingBaseTypes.avdl"));
        Files.copy(testDirectory.resolve("DomainEventBaseTypes.avdl"), targetClasses.resolve("DomainEventBaseTypes.avdl"));
    }

    @AfterEach
    void deleteTestRepo() throws IOException {
        testRepo.delete();
    }

    /**
     * Point the project and mojo at the test repo directory so paths resolve correctly.
     * The project is already the same instance injected into the mojo by @InjectMojo,
     * so updating its basedir also affects the mojo's runtime behavior.
     */
    private void configureMojo(MessageTypesCompilerMojo myMojo) throws IllegalAccessException {
        File testDir = testRepo.repoDir().toFile();
        MavenProject project = new MavenProject();
        setVariableValueToObject(project, "basedir", testDir);
        project.getBuild().setDirectory(new File(testDir, "target").getAbsolutePath());
        project.getBuild().setOutputDirectory(new File(testDir, "target/classes").getAbsolutePath());
        setVariableValueToObject(myMojo, "project", project);
        setVariableValueToObject(myMojo, "sourceDirectory", new File(testDir, "descriptor"));
        setVariableValueToObject(myMojo, "outputDirectory", new File(testDir, "target/generated-sources"));

        myMojo.setGenerateAllMessageTypes(false);
        myMojo.setCurrentBranch("master");
        myMojo.setTrunkBranchName("master");
        myMojo.setCommitId(testRepo.commits().get(testRepo.commits().size() - 1).name());
        myMojo.setGitUrl(testRepo.url());
        myMojo.setGroupIdPrefix("ch.bit.admin.test");
    }

    @Test
    @InjectMojo(goal = "compile-message-types", pom = "src/test/resources/sample-message-type-registry/pom.xml")
    void execute_diff_noNewMessageTypes(MessageTypesCompilerMojo myMojo) throws Exception {
        configureMojo(myMojo);
        testRepo.checkoutCommit(0);
        myMojo.execute();
        assertFileDoesNotExist("target/generated-sources/activ");
    }

    @Test
    @InjectMojo(goal = "compile-message-types", pom = "src/test/resources/sample-message-type-registry/pom.xml")
    void execute_diff_singleNewMessageType_noOldDescriptor(MessageTypesCompilerMojo myMojo) throws Exception {
        configureMojo(myMojo);
        testRepo.checkoutCommit(1);
        myMojo.execute();

        assertFileExists("target/generated-sources/activ/_common/src/main/java/ch/admin/bazg/activ/test/common/SystemCommon.java");
        assertFileExists("target/generated-sources/activ/ActivZoneEnteredEvent/1.0.0/src/main/java/ch/admin/bazg/activ/test/event/ActivZoneEnteredEvent.java");
        assertFileDoesNotExist("target/generated-sources/activ/ActivZoneEnteredEvent/2.0.0/src/main/java/ch/admin/bazg/activ/test/event/v2/ActivZoneEnteredEvent.java");
    }

    @Test
    @InjectMojo(goal = "compile-message-types", pom = "src/test/resources/sample-message-type-registry/pom.xml")
    void execute_diff_singleNewMessageType_existingTypeShouldNotBeGenerated(MessageTypesCompilerMojo myMojo) throws Exception {
        configureMojo(myMojo);
        testRepo.repo().tag()
                .setObjectId(testRepo.commits().get(1))
                .setName("v1.0.0")
                .call();

        testRepo.checkoutCommit(2);
        myMojo.execute();

        assertFileExists("target/generated-sources/activ/_common/src/main/java/ch/admin/bazg/activ/test/common/SystemCommon.java");
        assertFileDoesNotExist("target/generated-sources/activ/ActivZoneEnteredEvent/1.0.0/src/main/java/ch/admin/bazg/activ/test/event/ActivZoneEnteredEvent.java");
        assertFileExists("target/generated-sources/activ/ActivZoneEnteredEvent/2.0.0/src/main/java/ch/admin/bazg/activ/test/event/v2/ActivZoneEnteredEvent.java");
    }

    @Test
    @InjectMojo(goal = "compile-message-types", pom = "src/test/resources/sample-message-type-registry/pom.xml")
    void execute_diff_newCommand(MessageTypesCompilerMojo myMojo) throws Exception {
        configureMojo(myMojo);
        testRepo.repo().tag()
                .setObjectId(testRepo.commits().get(2))
                .setName("v2.0.0")
                .call();

        testRepo.checkoutCommit(3);
        myMojo.execute();

        assertFileDoesNotExist("target/generated-sources/activ");
        assertFileExists("target/generated-sources/autorisaziun/AutorisaziunCancelPermitConsumptionCommand/1.0.0/src/main/java/ch/admin/bazg/autorisaziun/common/api/domain/command/permit/AutorisaziunCancelPermitConsumptionCommand.java");
    }

    @Test
    @InjectMojo(goal = "compile-message-types", pom = "src/test/resources/sample-message-type-registry/pom.xml")
    void execute_diff_twoNewMessageTypes(MessageTypesCompilerMojo myMojo) throws Exception {
        configureMojo(myMojo);
        testRepo.checkoutCommit(2);
        myMojo.execute();

        assertFileExists("target/generated-sources/activ/_common/src/main/java/ch/admin/bazg/activ/test/common/SystemCommon.java");
        assertFileExists("target/generated-sources/activ/ActivZoneEnteredEvent/1.0.0/src/main/java/ch/admin/bazg/activ/test/event/ActivZoneEnteredEvent.java");
        assertFileExists("target/generated-sources/activ/ActivZoneEnteredEvent/2.0.0/src/main/java/ch/admin/bazg/activ/test/event/v2/ActivZoneEnteredEvent.java");
    }

    @Test
    @InjectMojo(goal = "compile-message-types", pom = "src/test/resources/sample-message-type-registry/pom.xml")
    void execute_diff_withGitToken(MessageTypesCompilerMojo myMojo, EnvironmentVariables environmentVariables) throws Exception {
        configureMojo(myMojo);
        environmentVariables.set("MESSAGE_TYPE_REPO_GIT_TOKEN", "test-token-value");
        testRepo.repo().tag()
                .setObjectId(testRepo.commits().get(2))
                .setName("v2.0.0")
                .call();
        testRepo.checkoutCommit(3);

        myMojo.execute();

        assertFileDoesNotExist("target/generated-sources/activ");
        assertFileExists("target/generated-sources/autorisaziun/AutorisaziunCancelPermitConsumptionCommand/1.0.0/src/main/java/ch/admin/bazg/autorisaziun/common/api/domain/command/permit/AutorisaziunCancelPermitConsumptionCommand.java");
    }

    private void assertFileExists(String file) {
        Path path = testRepo.repoDir().resolve(file);
        assertTrue(Files.exists(path), "file " + file + " exists");
    }

    private void assertFileDoesNotExist(String file) {
        Path path = testRepo.repoDir().resolve(file);
        assertFalse(Files.exists(path), "file " + file + " does not exist");
    }
}
