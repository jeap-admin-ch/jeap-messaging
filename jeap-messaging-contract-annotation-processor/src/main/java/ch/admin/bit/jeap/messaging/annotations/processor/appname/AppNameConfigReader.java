package ch.admin.bit.jeap.messaging.annotations.processor.appname;

import ch.admin.bit.jeap.messaging.annotations.processor.MessagingAnnotationProcessingException;
import lombok.SneakyThrows;

import javax.lang.model.element.Element;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

class AppNameConfigReader {

    private static final String[] CONFIG_FILENAMES = {
            "bootstrap.properties",
            "application.properties",
            "bootstrap.yml",
            "bootstrap.yaml",
            "application.yml",
            "application.yaml"};
    private static final String CONFIG_FILE_LOCATION = Paths.get("src", "main", "resources").toString();
    private static final int MAX_DEPTH = 32;

    /**
     * Finds a spring.application.name property in an application/boostrap.y[a]ml file on the classpath
     */
    static Optional<String> findSpringApplicationNameInClasspathResources(Element annotatedElement) {
        Set<String> springApplicationNames = Arrays.stream(CONFIG_FILENAMES)
                .flatMap(name -> findSpringApplicationNameInClasspathResource(name).stream())
                .collect(toSet());
        return requireUniqueApplicationName(springApplicationNames, annotatedElement);
    }

    static Optional<String> findSpringApplicationNameInConfigFiles(Path projectRootDirectory, Element annotatedElement) {
        Set<String> springApplicationNames = AppNameConfigReader.findSpringApplicationNames(projectRootDirectory);
        return requireUniqueApplicationName(springApplicationNames, annotatedElement);
    }

    static Set<String> findSpringApplicationNames(Path workingDirectory) {
        try (Stream<Path> pathStream = findConfigFiles(workingDirectory)) {
            return pathStream
                    .map(ConfigParser::getSpringApplicationName)
                    .filter(Objects::nonNull)
                    .collect(toSet());
        }
    }

    @SneakyThrows
    private static Stream<Path> findConfigFiles(Path workingDirectory) {
        return Files.find(workingDirectory, MAX_DEPTH, AppNameConfigReader::isConfigFile);
    }

    private static boolean isConfigFile(Path path, BasicFileAttributes attr) {
        return attr.isRegularFile() && isConfigFileName(path);
    }

    private static boolean isConfigFileName(Path path) {
        String filenameString = path.getFileName().toString();
        String pathString = path.toString();
        for (String configFilename : CONFIG_FILENAMES) {
            boolean isSpringBootConfigFile = filenameString.equals(configFilename);
            boolean isProductiveSourceFile = pathString.contains(CONFIG_FILE_LOCATION);
            if (isSpringBootConfigFile && isProductiveSourceFile) {
                return true;
            }
        }
        return false;
    }

    static Optional<String> findSpringApplicationNameInClasspathResource(String fileName) {
        ClassLoader classLoader = AppNameConfigReader.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(fileName)) {
            if (inputStream == null) {
                return Optional.empty();
            }
            String springApplicationName = ConfigParser.getSpringApplicationName(fileName, inputStream);

            return Optional.ofNullable(springApplicationName);
        } catch (IOException e) {
            // Invalid file - ignore
            return Optional.empty();
        }
    }

    private static Optional<String> requireUniqueApplicationName(Set<String> springApplicationNames, Element annotatedElement) {
        if (springApplicationNames.size() > 1) {
            throw MessagingAnnotationProcessingException.multipleAppNamesFound(springApplicationNames, annotatedElement);
        }
        return springApplicationNames.stream().findFirst();
    }
}
