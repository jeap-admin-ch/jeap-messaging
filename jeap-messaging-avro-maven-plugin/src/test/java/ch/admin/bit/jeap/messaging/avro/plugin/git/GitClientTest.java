package ch.admin.bit.jeap.messaging.avro.plugin.git;

import ch.admin.bit.jeap.messaging.avro.pluginIntegration.repo.TestRegistryRepo;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.assertj.core.api.Assertions;
import org.eclipse.jgit.lib.Ref;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitClientTest {

    private TestRegistryRepo testRepo;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        testRepo = TestRegistryRepo.testRepoWithThreeCommitsAddingMessageTypeV1AndV2AndCommand();
    }

    @AfterEach
    @SneakyThrows
    void tearDown() {
        testRepo.delete();
    }

    @Test
    void findMostRecentTag_semantic() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("1.0.0"),
                        createMockRef("1.2.3"),
                        createMockRef("1.15.3"),
                        createMockRef("1.1.1")),
                "1.15.3");
    }

    @Test
    void findMostRecentTag_buildTimestamp() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("1.0.0-20240208133822"),
                        createMockRef("1.0.0-20240208133823"),
                        createMockRef("1.0.0-20240208133826"),
                        createMockRef("1.0.0-20240108133826")),
                "1.0.0-20240208133826");
    }

    @Test
    void findMostRecentTag_buildTimestampNewVersion() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("1.0.0-20240208133822"),
                        createMockRef("1.0.0-20240208133823"),
                        createMockRef("1.0.0-20240208133826"),
                        createMockRef("1.1.0-20240208133826"),
                        createMockRef("1.0.0-20240108133826")),
                "1.1.0-20240208133826");
    }

    @Test
    void findMostRecentTag_buildNumber() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("1.0.1-53"),
                        createMockRef("1.2.0-67"),
                        createMockRef("1.2.0-68"),
                        createMockRef("1.1.9-99")),
                "1.2.0-68");
    }

    @Test
    void findMostRecentTag_buildMultiple() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("1.30.1-53"),
                        createMockRef("1.29.0-67"),
                        createMockRef("1.4.0-68"),
                        createMockRef("1.9.9-99")),
                "1.30.1-53");
    }

    @Test
    void findMostRecentTag_snapshot() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("0.0.1-657"),
                        createMockRef("0.0.1-SNAPSHOT"),
                        createMockRef("0.0.2-SNAPSHOT"),
                        createMockRef("0.0.1-68"),
                        createMockRef("1.0.0-SNAPSHOT")),
                "0.0.1-657");
    }

    @Test
    void findMostRecentTag_ignoreUnknownTags() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("2.0.0-20240208133822"),
                        createMockRef("foo-bar"),
                        createMockRef("foo.bar"),
                        createMockRef("2.dummy.6"),
                        createMockRef("1.98788123"),
                        createMockRef("0.0.443343"),
                        createMockRef("dummy"),
                        createMockRef("1.0-alpha")),
                "2.0.0-20240208133822");
    }

    @Test
    void findMostRecentTag_doRemoveV_vIsNew() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("v4.93.4"),
                        createMockRef("v4.93.3"),
                        createMockRef("4.93.2"),
                        createMockRef("4.93.1"),
                        createMockRef("4.93.0")),
                "v4.93.4");
    }

    @Test
    void findMostRecentTag_doRemoveV_vIsNew_NotSorted() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("v4.93.3"),
                        createMockRef("4.93.2"),
                        createMockRef("v4.93.4"),
                        createMockRef("4.93.0"),
                        createMockRef("4.93.1")
                ),
                "v4.93.4");
    }

    @Test
    void findMostRecentTag_doRemoveV_vIsNotNew() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("4.93.4"),
                        createMockRef("4.93.3"),
                        createMockRef("v4.93.2"),
                        createMockRef("v4.93.1"),
                        createMockRef("v4.93.0")),
                "4.93.4");
    }

    @Test
    @SneakyThrows
    void fetchTags_WhenUsingSystemAndGitProcessThrowsIOException_thenThrowsMojoExecutionException() {
        final String remoteUrl = "some-remote-url";
        ProcessBuilderFactory processBuilderFactory = mock(ProcessBuilderFactory.class);
        ProcessBuilder processBuilder = mock(ProcessBuilder.class);
        when(processBuilderFactory.createProcessBuilder("git", "fetch", "--tags", "--force", remoteUrl)).thenReturn(processBuilder);
        when(processBuilder.start()).thenThrow(new IOException());
        GitClient gitClient = new GitClient(testRepo.repoDir().toString(), remoteUrl, "some-trunk-branch-name", new SystemStreamLog(), processBuilderFactory);

        Assertions.assertThatThrownBy(
                        () -> gitClient.gitFetchTags(null) // no token -> system git is used
                )
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageStartingWith("Failed to fetch the tags from the remote repository using a Git process");
    }

    @Test
    @SneakyThrows
    void fetchTags_WhenUsingSystemAndGitProcessIsInterrupted_thenThrowsMojoExecutionException() {
        final String remoteUrl = "some-remote-url";
        ProcessBuilderFactory processBuilderFactory = mock(ProcessBuilderFactory.class);
        ProcessBuilder processBuilder = mock(ProcessBuilder.class);
        when(processBuilderFactory.createProcessBuilder("git", "fetch", "--tags", "--force", remoteUrl)).thenReturn(processBuilder);
        Process process = mock(Process.class);
        when(processBuilder.start()).thenReturn(process);
        when(process.waitFor()).thenThrow(new InterruptedException());
        GitClient gitClient = new GitClient(testRepo.repoDir().toString(), remoteUrl, "some-trunk-branch-name", new SystemStreamLog(), processBuilderFactory);

        Assertions.assertThatThrownBy(
                        () -> gitClient.gitFetchTags(null) // no token -> system git is used
                )
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageStartingWith("Interrupted while waiting for the Git fetch tags process to finish");
    }

    @Test
    @SneakyThrows
    void fetchTags_WhenUsingSystemAndGitAndProcessReturnsNonZero_thenThrowsMojoExecutionException() {
        final String remoteUrl = "some-remote-url";
        ProcessBuilderFactory processBuilderFactory = mock(ProcessBuilderFactory.class);
        ProcessBuilder processBuilder = mock(ProcessBuilder.class);
        when(processBuilderFactory.createProcessBuilder("git", "fetch", "--tags", "--force", remoteUrl)).thenReturn(processBuilder);
        Process process = mock(Process.class);
        when(processBuilder.start()).thenReturn(process);
        when(process.waitFor()).thenReturn(42);
        GitClient gitClient = new GitClient(testRepo.repoDir().toString(), remoteUrl, "some-trunk-branch-name", new SystemStreamLog(), processBuilderFactory);

        Assertions.assertThatThrownBy(
                        () -> gitClient.gitFetchTags(null) // no token -> system git is used
                )
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageStartingWith("The Git fetch process failed to fetch the tags from the remote repository");
    }

    private void doFindMostRecentTag(List<Ref> tags, String expectedResult) throws MojoExecutionException {
        Ref mostRecentTag = GitClient.findMostRecentTag(tags);
        assertThat(mostRecentTag.getName()).isEqualTo(expectedResult);
    }

    private Ref createMockRef(String name) {
        Ref ref = mock(Ref.class);
        when(ref.getName()).thenReturn(name);
        return ref;
    }


}
