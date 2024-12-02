package ch.admin.bit.jeap.messaging.avro.plugin.git;

import ch.admin.bit.jeap.messaging.avro.plugin.helper.TypeDescriptorFactory;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.TypeDescriptor;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.TypeVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

class NewMessageTypeDiff {

    /**
     * @return The set of message types defined in a commit, compared to a base commit
     */
    static Set<NewMessageTypeVersionDto> findNewMessageTypeVersions(
            Path sourceDir, String descriptorPath, Git git, RevCommit newCommit, RevCommit baseCommit) throws MojoExecutionException {
        try {
            String newDescriptor = getContent(newCommit, descriptorPath, git);
            String baseDescriptor = getContent(baseCommit, descriptorPath, git);
            TypeDescriptor newTypeDescriptor = TypeDescriptorFactory.readTypeDescriptor(newDescriptor, descriptorPath);
            TypeDescriptor baseTypeDescriptor = TypeDescriptorFactory.readTypeDescriptor(baseDescriptor, descriptorPath);

            return diffMessageTypes(sourceDir, Path.of(descriptorPath), newTypeDescriptor, baseTypeDescriptor);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to read descriptor " + descriptorPath, ex);
        }
    }

    static Set<NewMessageTypeVersionDto> diffMessageTypes(Path sourceDir, Path descriptorPath,
                                                          TypeDescriptor newTypeDescriptor,
                                                          TypeDescriptor baseTypeDescriptor) {
        Set<String> newVersions = getVersionNumbers(newTypeDescriptor);
        Set<String> baseVersions = getVersionNumbers(baseTypeDescriptor);
        newVersions.removeAll(baseVersions);
        String systemName = systemName(descriptorPath);

        Path absoluteDescriptorPath = sourceDir.resolve(descriptorPath);

        return newVersions.stream()
                .map(v -> new NewMessageTypeVersionDto(systemName, absoluteDescriptorPath, newTypeDescriptor, v))
                .collect(toSet());
    }

    /**
     * @return the system name part of a descriptor path (descriptor/sys/.. --> "sys")
     */
    private static String systemName(Path path) {
        return path.subpath(1, 2).toString().toLowerCase(Locale.ROOT);
    }

    private static Set<String> getVersionNumbers(TypeDescriptor typeDescriptor) {
        if (typeDescriptor == null) {
            return Set.of();
        }
        return typeDescriptor.getVersions().stream()
                .map(TypeVersion::getVersion)
                .collect(toCollection(HashSet::new));
    }

    /**
     * @return File content as String if found, null otherwise
     */
    private static String getContent(RevCommit commit, String descriptorPath, Git git) throws IOException {
        Repository repository = git.getRepository();
        RevTree tree = commit.getTree();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            if (!findFile(descriptorPath, treeWalk)) {
                return null;
            }

            ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
            return new String(loader.getBytes(), StandardCharsets.UTF_8);
        }
    }

    private static boolean findFile(String descriptorPath, TreeWalk treeWalk) throws IOException {
        while (treeWalk.next()) {
            if (treeWalk.getPathString().equals(descriptorPath)) {
                return true;
            }
        }
        return false;
    }
}
