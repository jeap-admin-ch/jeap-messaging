package ch.admin.bit.jeap.messaging.avro.plugin.git;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled("Only runs locally")
class GitClientIT {

    @Test
    void diffCommit() throws MojoExecutionException {
        GitClient gitClient = new GitClient("/home/dev/development/projects/jme-message-type-registry", "master");
        final GitDiffDto gitDiff = gitClient.getGitDiff("feature");
        assertNotNull(gitDiff);
    }

    @Test
    void getDiffFromLastTag() throws MojoExecutionException {
        GitClient gitClient = new GitClient("/home/dev/development/projects/jme-message-type-registry", "master");
        final GitDiffDto gitDiff = gitClient.getDiffFromLastTag();
        assertNotNull(gitDiff);
    }

    @Test
    void getDiffFromMaster() throws MojoExecutionException {
        GitClient gitClient = new GitClient("/home/dev/development/projects/jme-message-type-registry", "master");
        final GitDiffDto gitDiff = gitClient.getDiffFromTrunk();
        assertNotNull(gitDiff);
    }

}
