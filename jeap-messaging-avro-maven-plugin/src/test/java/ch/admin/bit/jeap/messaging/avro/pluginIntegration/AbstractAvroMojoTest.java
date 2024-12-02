package ch.admin.bit.jeap.messaging.avro.pluginIntegration;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

public abstract class AbstractAvroMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    void assertAllCommonEventFilesRemoved(List<String> filenames) {
        List<String> errorNames = filenames.stream()
                .filter(filename -> filename.matches("^.*Avro(Domain|Message)\\w+\\.java$"))
                .collect(Collectors.toList());
        assertEquals(emptyList(), errorNames);
    }

    File syncWithNewTempDirectory(final String srcTestDirectory) throws IOException {
        File tmpTestDir = temporaryFolder.newFolder("test");
        final File testPomDir = new File(srcTestDirectory);
        FileUtils.copyDirectory(testPomDir, tmpTestDir);
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
