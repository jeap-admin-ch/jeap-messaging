package ch.admin.bit.jeap.messaging.registry;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MessageTypeRegistryVerifierCompatibilityMojoTest extends AbstractMojoTestCase {
    private final static File RESOURCES_DIR = new File("src/test/resources/");

    @BeforeEach
    void setupTestDir() throws Exception {
        setUp();
    }

    @Test
    void validateNewTypesOnly(@TempDir File tmpDir) throws Exception {
        String gitUrl = createTestRepoFrom(Path.of("src/test/resources/validateNewTypesOnly_oldRepo"));

        File testDir = new File(RESOURCES_DIR, "validateNewTypesOnly");
        FileUtils.copyDirectory(testDir, tmpDir);
        MessageTypeRegistryVerifierMojo mojo = (MessageTypeRegistryVerifierMojo) open(tmpDir);
        mojo.setGitUrl(gitUrl);
        assertThatThrownBy(mojo::execute)
                .hasMessageContaining("(1 errors)")
                .hasMessageContaining("Schemas TestTestEvent version 1.3.0 and 1.1.0 are not backward compatible");
    }

    @Test
    void compatibleChange(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "compatibleChange");
        FileUtils.copyDirectory(testDir, tmpDir);
        MessageTypeRegistryVerifierMojo mojo = (MessageTypeRegistryVerifierMojo) open(tmpDir);
        mojo.setGitUrl(createEmptyTestRepo());
        assertDoesNotThrow(mojo::execute);
    }

    @Test
    void incompatibleChange(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "changeCompatibilityCheck");
        FileUtils.copyDirectory(testDir, tmpDir);
        MessageTypeRegistryVerifierMojo mojo = (MessageTypeRegistryVerifierMojo) open(tmpDir);
        mojo.setGitUrl(createEmptyTestRepo());

        assertThatThrownBy(mojo::execute)
                .hasMessageContaining("(2 errors)")
                .hasMessageContaining("Schemas TestTestEvent version 1.2.0 and 1.1.0 are not backward compatible")
                .hasMessageContaining("Schemas TestTestEvent version 1.3.0 and 1.1.0 are not forward compatible");
    }

    @Test
    void missingCompatibilityMode(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "missingCompatibilityMode");
        FileUtils.copyDirectory(testDir, tmpDir);
        MessageTypeRegistryVerifierMojo mojo = (MessageTypeRegistryVerifierMojo) open(tmpDir);
        mojo.setGitUrl(createEmptyTestRepo());

        assertThatThrownBy(mojo::execute)
                .hasMessageContaining("(2 errors)")
                .hasMessageContaining("The new message type TestTestEvent in version 1.2.0 is missing the mandatory 'compatibilityMode' attribute")
                .hasMessageContaining("The new message type TestTestCommand in version 2.0.0 is missing the mandatory 'compatibilityMode' attribute");
    }

    private Mojo open(File basedir) throws Exception {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory(basedir);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession(new DefaultRepositorySystemSession());

        File pom = new File(basedir, "pom.xml");
        MavenProject project = getContainer().lookup(ProjectBuilder.class).build(pom, configuration).getProject();
        MavenSession session = newMavenSession(project);
        MojoExecution execution = newMojoExecution("registry");
        return lookupConfiguredMojo(session, execution);
    }

    private String createTestRepoFrom(Path copyFrom) throws Exception {
        Path repoDir = Files.createTempDirectory("test-repo");

        // Init file-based repository, and copy/commit test files
        Git repo = initGitRepo(repoDir);
        copyTestRegistryFilesToRepositoryDir(copyFrom, repoDir);
        addAndCommitTestFiles(repo, "commit-0");
        repo.tag()
                .setName("v1.0.0")
                .call();
        return repoDir.toUri().toString();
    }

    private String createEmptyTestRepo() throws Exception {
        Path repoDir = Files.createTempDirectory("test-repo");

        // Init file-based repository, and copy/commit test files
        Git repo = initGitRepo(repoDir);
        addAndCommitTestFiles(repo, "commit-0");
        repo.tag()
                .setName("v1.0.0")
                .call();
        return repoDir.toUri().toString();
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
}
