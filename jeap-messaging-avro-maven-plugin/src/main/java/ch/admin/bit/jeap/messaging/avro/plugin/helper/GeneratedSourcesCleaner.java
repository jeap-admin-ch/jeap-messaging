package ch.admin.bit.jeap.messaging.avro.plugin.helper;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class GeneratedSourcesCleaner {

    private final Log log;

    public GeneratedSourcesCleaner() {
        this.log = new SystemStreamLog();
    }

    public void cleanupDuplicatedCommonFiles(File outputDirectory, Map<String, List<Path>> commonDefinitionsPerSystem) throws MojoExecutionException {
        if (!outputDirectory.exists()) {
            return;
        }

        log.debug("Cleanup duplicated common files");
        int countCleanUpFiles = 0;
        final List<Path> generatedSources = retrieveAllGeneratedClasses(outputDirectory);

        try {
            final List<String> commonFiles = commonDefinitionsPerSystem.values().stream().flatMap(List::stream)
                    .map(cf -> RegistryHelper.convertFileNameOfAVDLToFilePathOfJava(cf.getFileName().toString())).toList();

            for (Path current : generatedSources) {
                for (String alreadyCommonFile : commonFiles) {
                    if (current.endsWith(alreadyCommonFile) && !current.toString().contains(MessageTypeRegistryConstants.COMMON_DIR_NAME)) {
                        Files.deleteIfExists(current);
                        countCleanUpFiles++;
                        break;
                    }
                }
            }

            log.info("Cleanup deleted " + countCleanUpFiles + " duplicated files");
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot cleanup: " + e.getMessage(), e);
        }

    }

    private List<Path> retrieveAllGeneratedClasses(File classesDirectory) throws MojoExecutionException {
        try (Stream<Path> stream = Files.walk(Paths.get(classesDirectory.getAbsolutePath()), Integer.MAX_VALUE)) {
            return stream
                    .filter(path -> FilenameUtils.getExtension(path.getFileName().toString()).equals("java"))
                    .toList();
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot walk through classes directory: " + e.getMessage(), e);
        }
    }

}
