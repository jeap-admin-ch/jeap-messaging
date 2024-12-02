package ch.admin.bit.jeap.messaging.avro.pluginIntegration.repo;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record TestRegistryRepo(Path repoDir, Git repo, List<RevCommit> commits, String url) {

    public static TestRegistryRepo testRepoWithThreeCommitsAddingMessageTypeV1AndV2AndCommand() throws Exception {
        Path repoDir = Files.createTempDirectory("test-repo");

        // Init file-based repository, and copy/commit test files
        Git repo = initGitRepo(repoDir);

        // Commit 0: No message types, only pom and base types
        copyTestRegistryFilesToRepositoryDir(Path.of("src/test/resources/test-registry/commit-0"), repoDir);
        RevCommit commit0 = addAndCommitTestFiles(repo, "commit-0");

        // Tag commit 0 as the base to diff against for the first
        repo.tag()
                .setName("v0.0.1")
                .call();

        // Commit 1: Add event v1
        copyTestRegistryFilesToRepositoryDir(Path.of("src/test/resources/test-registry/commit-1"), repoDir);
        RevCommit commit1 = addAndCommitTestFiles(repo, "commit-1");

        // Commit 2: Add event v2
        copyTestRegistryFilesToRepositoryDir(Path.of("src/test/resources/test-registry/commit-2"), repoDir);
        RevCommit commit2 = addAndCommitTestFiles(repo, "commit-2");

        // Commit 3: Add command
        copyTestRegistryFilesToRepositoryDir(Path.of("src/test/resources/test-registry/commit-3"), repoDir);
        RevCommit commit3 = addAndCommitTestFiles(repo, "commit-3");

        List<RevCommit> commits = List.of(commit0, commit1, commit2, commit3);
        return new TestRegistryRepo(repoDir, repo, commits, repoDir.toUri().toString());
    }

    private static Git initGitRepo(Path repoDir) throws GitAPIException {
        return Git.init()
                .setDirectory(repoDir.toFile())
                .setGitDir(repoDir.resolve(".git").toFile())
                .setInitialBranch("master")
                .call();
    }

    private static RevCommit addAndCommitTestFiles(Git newRepo, String message) throws GitAPIException {
        newRepo.add()
                .addFilepattern(".")
                .call();
        return newRepo.commit()
                .setAuthor("test", "test@mail.com")
                .setMessage(message)
                .call();
    }

    private static void copyTestRegistryFilesToRepositoryDir(Path from, Path repoDir) throws IOException {
        FileUtils.copyDirectory(from.toFile(), repoDir.toFile());
    }

    public void delete() throws IOException {
        repo.close();
        FileUtils.forceDelete(repoDir.toFile());
    }

    public void checkoutCommit(int index) throws GitAPIException {
        repo.checkout()
                .setName(commits.get(index).name())
                .call();
    }
}
