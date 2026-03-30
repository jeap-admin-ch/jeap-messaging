package ch.admin.bit.jeap.messaging.avro.pluginIntegration;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class AbstractAvroMojoTest {

    void assertAllCommonEventFilesRemoved(List<String> filenames) {
        List<String> errorNames = filenames.stream()
                .filter(filename -> filename.matches("^.*Avro(Domain|Message)\\w+\\.java$"))
                .collect(Collectors.toList());
        assertEquals(emptyList(), errorNames);
    }

    void deleteTargetDir(String basedir) {
        try {
            File targetDir = new File(basedir, "target");
            if (targetDir.exists()) {
                FileUtils.deleteDirectory(targetDir);
            }
        } catch (IOException e) {
            // best effort cleanup
        }
    }

    File syncToTempDirectory(String srcTestDirectory, Path tempDir) throws IOException {
        File tmpTestDir = tempDir.resolve("test").toFile();
        Files.createDirectories(tmpTestDir.toPath());
        FileUtils.copyDirectory(new File(srcTestDirectory), tmpTestDir);
        return tmpTestDir;
    }

    List<String> readAllFiles(File testPomDir) throws IOException {
        try (Stream<Path> stream = Files.walk(testPomDir.toPath(), Integer.MAX_VALUE)) {
            return stream
                    .filter(p -> !Files.isDirectory(p))
                    .map(String::valueOf)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}
