package ch.admin.bit.jeap.messaging.avro.plugin.git;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GitClient {

    private final Log log;
    private final Repository repo;
    private final Git git;
    private final String remoteUrl;
    private final String trunkBranchName;
    private final String sourceDirectory;

    public GitClient(String sourceDirectory, String remoteUrl, String trunkBranchName) throws MojoExecutionException {
        this.sourceDirectory = sourceDirectory;
        this.log = new SystemStreamLog();
        this.remoteUrl = remoteUrl;
        this.trunkBranchName = trunkBranchName;
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            this.repo = builder.setGitDir(new File(sourceDirectory + "/.git")).setMustExist(true).build();
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot build repo " + e.getMessage(), e);
        }
        this.git = new Git(repo);
    }

    public GitDiffDto getGitDiff(String branchName) throws MojoExecutionException {
        if (trunkBranchName.equals(branchName)) {
            return getDiffFromLastTag();
        } else {
            return getDiffFromTrunk();
        }
    }

    protected GitDiffDto getDiffFromLastTag() throws MojoExecutionException {
        log.debug("Retrieve Git Diff from last tag...");
        return executeDiffBetweenCommits(retrieveLastCommitFromLastTag(), retrieveLastCommitFromCurrentBranch());
    }

    protected GitDiffDto getDiffFromTrunk() throws MojoExecutionException {
        log.debug("Retrieve Git Diff from " + trunkBranchName + " branch...");
        return executeDiffBetweenCommits(retrieveLastCommitFromTrunk(), retrieveLastCommitFromCurrentBranch());
    }

    private RevCommit retrieveLastCommitFromCurrentBranch() throws MojoExecutionException {
        try (RevWalk walk = new RevWalk(repo)) {
            return walk.parseCommit(git.log().setMaxCount(1).call().iterator().next());
        } catch (IOException | GitAPIException e) {
            log.error("Cannot retrieve current commit " + e.getMessage(), e);
            throw new MojoExecutionException("Cannot retrieve current commit " + e.getMessage(), e);
        }
    }

    private RevCommit retrieveLastCommitFromTrunk() throws MojoExecutionException {
        try {
            return git.log().add(repo.resolve("remotes/origin/" + trunkBranchName)).setMaxCount(1).call().iterator().next();
        } catch (IOException | GitAPIException e) {
            String errorMessage = "Cannot retrieve last commit from " + trunkBranchName + ": " + e.getMessage();
            log.error(errorMessage, e);
            throw new MojoExecutionException(errorMessage, e);
        }
    }

    private RevCommit retrieveLastCommitFromLastTag() throws MojoExecutionException {
        try {
            log.debug("get remote tags with remote url " + this.remoteUrl);
            Collection<Ref> tags = Git.lsRemoteRepository()
                    .setTags(true)
                    .setRemote(this.remoteUrl)
                    .call();
            log.debug("found " + tags.size() + " tags");

            //Find the most recent tag from the reversed sorted list
            final Ref lastTag = findMostRecentTag(tags);
            log.debug("last tag: " + lastTag.getName());

            // fetch all commits for this tag
            final LogCommand logFromTag = git.log();

            Ref peeledRef = repo.getRefDatabase().peel(lastTag);
            if (peeledRef.getPeeledObjectId() != null) {
                logFromTag.add(peeledRef.getPeeledObjectId());
            } else {
                logFromTag.add(lastTag.getObjectId());
            }

            return logFromTag.setMaxCount(1).call().iterator().next();

        } catch (IOException | GitAPIException e) {
            log.error("Cannot retrieve last tag commits " + e.getMessage(), e);
            throw new MojoExecutionException("Cannot retrieve last tag commits " + e.getMessage(), e);
        }
    }

    protected static Ref findMostRecentTag(Collection<Ref> tags) throws MojoExecutionException {
        final RefComparableVersion lastTag = tags.stream().filter(t -> !t.getName().contains("-SNAPSHOT"))
                .map(RefComparableVersion::new)
                .max(RefComparableVersion::compareTo)
                .orElseThrow(() -> new MojoExecutionException("Cannot find the most recent tag from the tags list"));
        return lastTag.getRef();
    }

    private GitDiffDto executeDiffBetweenCommits(RevCommit baseCommit, RevCommit newCommit) throws MojoExecutionException {

        try {
            log.debug("LogCommit: " + baseCommit);
            log.debug("LogMessage: " + baseCommit.getFullMessage());

            log.debug("LogCommit: " + newCommit);
            log.debug("LogMessage: " + newCommit.getFullMessage());

            AbstractTreeIterator oldTreeIterator = getCanonicalTreeParser(git, baseCommit);
            AbstractTreeIterator newTreeIterator = getCanonicalTreeParser(git, newCommit);

            List<DiffEntry> deltas = git.diff()
                    .setOldTree(oldTreeIterator)
                    .setNewTree(newTreeIterator)
                    .call().stream()
                    .filter(GitClient::isDescriptor)
                    .toList();

            Set<NewMessageTypeVersionDto> newMessageTypeVersions = new HashSet<>();

            for (DiffEntry delta : deltas) {
                String changedFilePath = delta.getNewPath();
                Set<NewMessageTypeVersionDto> versions = NewMessageTypeDiff.findNewMessageTypeVersions(
                        Path.of(sourceDirectory), changedFilePath, git, newCommit, baseCommit);
                newMessageTypeVersions.addAll(versions);
            }

            return new GitDiffDto(newMessageTypeVersions);

        } catch (IOException e) {
            log.error("Cannot read tree " + e.getMessage(), e);
            throw new MojoExecutionException("Cannot read tree " + e.getMessage(), e);
        } catch (GitAPIException e) {
            log.error("Cannot retrieve Git information " + e.getMessage(), e);
            throw new MojoExecutionException("Cannot retrieve Git information " + e.getMessage(), e);
        }

    }

    private static boolean isDescriptor(DiffEntry d) {
        return d.getNewPath().startsWith("descriptor/") && d.getNewPath().endsWith(".json");
    }

    private AbstractTreeIterator getCanonicalTreeParser(Git git, ObjectId commitId) throws IOException {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            RevCommit commit = walk.parseCommit(commitId);
            ObjectId treeId = commit.getTree().getId();
            try (ObjectReader reader = git.getRepository().newObjectReader()) {
                return new CanonicalTreeParser(null, reader, treeId);
            }
        }
    }
}
