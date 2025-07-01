package ch.admin.bit.jeap.messaging.avro.pluginIntegration;

import ch.admin.bit.jeap.messaging.avro.plugin.mojo.MessageTypesCompilerMojo;
import ch.admin.bit.jeap.messaging.avro.pluginIntegration.repo.TestRegistryRepo;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MessageTypesCompilerGitDiffMojoTest extends AbstractAvroMojoTest {

    private TestRegistryRepo testRepo;

    @Rule
    public MojoRule mojoRule = new MojoRule();
    
    @Rule
    public EnvironmentVariablesRule environmentVariables = new EnvironmentVariablesRule();

    private MessageTypesCompilerMojo myMojo;

    @Before
    public void createTestRepo() throws Exception {
        testRepo = TestRegistryRepo.testRepoWithThreeCommitsAddingMessageTypeV1AndV2AndCommand();
        Path testDirectory = testRepo.repoDir();

        //with the real plugin these 2 definitions are available in the classpath
        Path targetClasses = testDirectory.resolve("target/classes");
        Files.createDirectories(targetClasses);
        Files.copy(testDirectory.resolve("MessagingBaseTypes.avdl"), targetClasses.resolve("MessagingBaseTypes.avdl"));
        Files.copy(testDirectory.resolve("DomainEventBaseTypes.avdl"), targetClasses.resolve("DomainEventBaseTypes.avdl"));

        myMojo = (MessageTypesCompilerMojo) mojoRule.lookupConfiguredMojo(
                testRepo.repoDir().toFile(), "compile-message-types");

        myMojo.setGenerateAllMessageTypes(false);
        myMojo.setCurrentBranch("master");
        myMojo.setTrunkBranchName("master");
        myMojo.setCommitId(testRepo.commits().get(testRepo.commits().size() - 1).name());
        myMojo.setGitUrl(testRepo.url());
        myMojo.setGroupIdPrefix("ch.bit.admin.test");
    }

    @After
    public void deleteTestRepo() throws IOException {
        testRepo.delete();
    }

    @Test
    public void execute_diff_noNewMessageTypes() throws Exception {
        // act
        testRepo.checkoutCommit(0);
        myMojo.execute();

        // assert nothing is generated
        assertFileDoesNotExist("target/generated-sources/activ");
    }

    @Test
    public void execute_diff_singleNewMessageType_noOldDescriptor() throws Exception {
        // act
        testRepo.checkoutCommit(1);
        myMojo.execute();

        // assert v1 is generated
        assertFileExists("target/generated-sources/activ/_common/src/main/java/ch/admin/bazg/activ/test/common/SystemCommon.java");
        assertFileExists("target/generated-sources/activ/ActivZoneEnteredEvent/1.0.0/src/main/java/ch/admin/bazg/activ/test/event/ActivZoneEnteredEvent.java");
        assertFileDoesNotExist("target/generated-sources/activ/ActivZoneEnteredEvent/2.0.0/src/main/java/ch/admin/bazg/activ/test/event/v2/ActivZoneEnteredEvent.java");

    }

    @Test
    public void execute_diff_singleNewMessageType_existingTypeShouldNotBeGenerated() throws Exception {
        // set last tag on commit 1
        // commit 2 will add v2 of the event compared to commit 1
        testRepo.repo().tag()
                .setObjectId(testRepo.commits().get(1))
                .setName("v1.0.0")
                .call();

        // act
        testRepo.checkoutCommit(2);
        myMojo.execute();

        // assert v1 not regenerated, v2 is generated
        assertFileExists("target/generated-sources/activ/_common/src/main/java/ch/admin/bazg/activ/test/common/SystemCommon.java");
        assertFileDoesNotExist("target/generated-sources/activ/ActivZoneEnteredEvent/1.0.0/src/main/java/ch/admin/bazg/activ/test/event/ActivZoneEnteredEvent.java");
        assertFileExists("target/generated-sources/activ/ActivZoneEnteredEvent/2.0.0/src/main/java/ch/admin/bazg/activ/test/event/v2/ActivZoneEnteredEvent.java");

    }

    @Test
    public void execute_diff_newCommand() throws Exception {
        // set last tag on commit 2
        // commit 3 will add a command
        testRepo.repo().tag()
                .setObjectId(testRepo.commits().get(2))
                .setName("v2.0.0")
                .call();

        // act
        testRepo.checkoutCommit(3);
        myMojo.execute();

        // assert activ not regenerated, command is generated
        assertFileDoesNotExist("target/generated-sources/activ");
        assertFileExists("target/generated-sources/autorisaziun/AutorisaziunCancelPermitConsumptionCommand/1.0.0/src/main/java/ch/admin/bazg/autorisaziun/common/api/domain/command/permit/AutorisaziunCancelPermitConsumptionCommand.java");
    }

    @Test
    public void execute_diff_twoNewMessageTypes() throws Exception {
        // act
        testRepo.checkoutCommit(2);
        myMojo.execute();

        // assert v1 and v2 are generated
        assertFileExists("target/generated-sources/activ/_common/src/main/java/ch/admin/bazg/activ/test/common/SystemCommon.java");
        assertFileExists("target/generated-sources/activ/ActivZoneEnteredEvent/1.0.0/src/main/java/ch/admin/bazg/activ/test/event/ActivZoneEnteredEvent.java");
        assertFileExists("target/generated-sources/activ/ActivZoneEnteredEvent/2.0.0/src/main/java/ch/admin/bazg/activ/test/event/v2/ActivZoneEnteredEvent.java");
    }

    @Test
    public void execute_diff_withGitToken() throws Exception {
        // Set the environment variable for the git token to make the plugin fetch tags using JGit and a token.
        // All other tests use the system git to fetch the tags as they don't set the token.
        environmentVariables.set("MESSAGE_TYPE_REPO_GIT_TOKEN", "test-token-value");
        // set last tag on commit 2
        testRepo.repo().tag()
                .setObjectId(testRepo.commits().get(2))
                .setName("v2.0.0")
                .call();
        // checkout commit 3, which adds a command
        testRepo.checkoutCommit(3);

        // act
        myMojo.execute();

        // assert activ not regenerated, command is generated
        assertFileDoesNotExist("target/generated-sources/activ");
        assertFileExists("target/generated-sources/autorisaziun/AutorisaziunCancelPermitConsumptionCommand/1.0.0/src/main/java/ch/admin/bazg/autorisaziun/common/api/domain/command/permit/AutorisaziunCancelPermitConsumptionCommand.java");
    }

    private void assertFileExists(String file) {
        Path path = testRepo.repoDir().resolve(file);
        assertTrue("file " + file + " exists", Files.exists(path));
    }

    private void assertFileDoesNotExist(String file) {
        Path path = testRepo.repoDir().resolve(file);
        assertFalse("file " + file + " does not exist", Files.exists(path));
    }
}
