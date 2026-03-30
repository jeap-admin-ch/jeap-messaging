package ch.admin.bit.jeap.messaging.registry;

import org.apache.commons.io.FileUtils;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@MojoTest
@ExtendWith(SystemStubsExtension.class)
class MessageTypeRegistryVerifierCompatibilityMojoTest {
    private static final File RESOURCES_DIR = new File("src/test/resources/");

    @Inject
    private MavenProject project;

    private void pointToTempDir(Mojo mojo, File tmpDir) throws IllegalAccessException {
        setVariableValueToObject(project, "basedir", tmpDir);
        project.getBuild().setDirectory(new File(tmpDir, "target").getAbsolutePath());
        project.getBuild().setOutputDirectory(new File(tmpDir, "target/classes").getAbsolutePath());
        setVariableValueToObject(mojo, "descriptorDirectory", new File(tmpDir, "descriptor"));
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void validateNewTypesOnly(Mojo target, @TempDir File tmpDir) throws Exception {
        String gitUrl = createTestRepoFrom(Path.of("src/test/resources/validateNewTypesOnly_oldRepo"));

        FileUtils.copyDirectory(new File(RESOURCES_DIR, "validateNewTypesOnly"), tmpDir);
        pointToTempDir(target, tmpDir);
        ((MessageTypeRegistryVerifierMojo) target).setGitUrl(gitUrl);
        assertThatThrownBy(target::execute)
                .hasMessageContaining("(1 errors)")
                .hasMessageContaining("Schemas TestTestEvent version 1.3.0 and 1.1.0 are not backward compatible");
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void compatibleChange_WithoutGitTokenConfigured(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "compatibleChange"), tmpDir);
        pointToTempDir(target, tmpDir);
        ((MessageTypeRegistryVerifierMojo) target).setGitUrl(createEmptyTestRepo());
        assertDoesNotThrow(target::execute);
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void compatibleChange_WithGitTokenConfigured(Mojo target, @TempDir File tmpDir, EnvironmentVariables env) throws Exception {
        env.set("MESSAGE_TYPE_REPO_GIT_TOKEN", "test-token-value");
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "compatibleChange"), tmpDir);
        pointToTempDir(target, tmpDir);
        ((MessageTypeRegistryVerifierMojo) target).setGitUrl(createEmptyTestRepo());
        assertDoesNotThrow(target::execute);
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void incompatibleChange(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "changeCompatibilityCheck"), tmpDir);
        pointToTempDir(target, tmpDir);
        ((MessageTypeRegistryVerifierMojo) target).setGitUrl(createEmptyTestRepo());

        assertThatThrownBy(target::execute)
                .hasMessageContaining("(2 errors)")
                .hasMessageContaining("Schemas TestTestEvent version 1.2.0 and 1.1.0 are not backward compatible")
                .hasMessageContaining("Schemas TestTestEvent version 1.3.0 and 1.1.0 are not forward compatible");
    }

    @Test
    @InjectMojo(goal = "registry", pom = "src/test/resources/valid/pom.xml")
    void missingCompatibilityMode(Mojo target, @TempDir File tmpDir) throws Exception {
        FileUtils.copyDirectory(new File(RESOURCES_DIR, "missingCompatibilityMode"), tmpDir);
        pointToTempDir(target, tmpDir);
        ((MessageTypeRegistryVerifierMojo) target).setGitUrl(createEmptyTestRepo());

        assertThatThrownBy(target::execute)
                .hasMessageContaining("(2 errors)")
                .hasMessageContaining("The new message type TestTestEvent in version 1.2.0 is missing the mandatory 'compatibilityMode' attribute")
                .hasMessageContaining("The new message type TestTestCommand in version 2.0.0 is missing the mandatory 'compatibilityMode' attribute");
    }

    private String createTestRepoFrom(Path copyFrom) throws Exception {
        Path repoDir = Files.createTempDirectory("test-repo");
        Git repo = initGitRepo(repoDir);
        copyTestRegistryFilesToRepositoryDir(copyFrom, repoDir);
        addAndCommitTestFiles(repo, "commit-0");
        repo.tag().setName("v1.0.0").call();
        return repoDir.toUri().toString();
    }

    private String createEmptyTestRepo() throws Exception {
        Path repoDir = Files.createTempDirectory("test-repo");
        Git repo = initGitRepo(repoDir);
        addAndCommitTestFiles(repo, "commit-0");
        repo.tag().setName("v1.0.0").call();
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
        newRepo.add().addFilepattern(".").call();
        return newRepo.commit()
                .setAuthor("test", "test@mail.com")
                .setMessage(message)
                .call();
    }

    private static void copyTestRegistryFilesToRepositoryDir(Path from, Path repoDir) throws IOException {
        FileUtils.copyDirectory(from.toFile(), repoDir.toFile());
    }
}
