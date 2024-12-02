package ch.admin.bit.jeap.messaging.annotations.processor.appname;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

class ProjectDirectoryLocator {

    private static final int MAX_DEPTH = 32;

    static Optional<Path> findModuleDirectory(Path annotatedSourceFile) {
        return findDirectoryContainingPom(annotatedSourceFile, true);
    }

    static Optional<Path> findProjectRootDirectory(Path annotatedSourceFile) {
        return findDirectoryContainingPom(annotatedSourceFile, false);
    }

    private static Optional<Path> findDirectoryContainingPom(Path annotatedSourceFile, boolean stopOnFirstMatch) {
        Path currentDirectory = annotatedSourceFile.getParent();
        Path pomFoundAt = null;
        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            if (directoryContainsPom(currentDirectory)) {
                // The current directory is a module or project root directory, contains a pom.xml
                pomFoundAt = currentDirectory;
                if (stopOnFirstMatch) {
                    break;
                }
            } else if (pomFoundAt != null) {
                // A pom.xml was found in a child directory, but not in the parent directory
                // At this point, stop traversing the directory tree and assume the project root directory has been found
                break;
            }
            currentDirectory = currentDirectory.getParent();
            if (currentDirectory == null) {
                break;
            }
        }

        return Optional.ofNullable(pomFoundAt);
    }

    private static boolean directoryContainsPom(Path currentPath) {
        return Files.exists(currentPath.resolve("pom.xml"));
    }
}
