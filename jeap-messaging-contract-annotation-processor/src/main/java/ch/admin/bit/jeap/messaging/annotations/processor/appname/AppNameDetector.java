package ch.admin.bit.jeap.messaging.annotations.processor.appname;

import ch.admin.bit.jeap.messaging.annotations.processor.MessagingAnnotationProcessingException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AppNameDetector {

    private static final Map<Integer, String> APP_NAME_BY_ELEMENT_CACHE = new ConcurrentHashMap<>();
    private static final Map<Path, String> APP_NAME_BY_MODULE_PATH_CACHE = new ConcurrentHashMap<>();
    private static final Map<Path, String> APP_NAME_BY_PROJECT_PATH_CACHE = new ConcurrentHashMap<>();

    private final SourceFiles sourceFiles;

    public AppNameDetector(ProcessingEnvironment processingEnvironment) {
        this.sourceFiles = new SourceFiles(processingEnvironment);
    }

    AppNameDetector(SourceFiles sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public String detectAppName(Element annotatedElement, String appNameFromAnnotation) {

        // 1. The appName attribute in the annotation takes precedence
        if (appNameFromAnnotation != null && !appNameFromAnnotation.isBlank()) {
            return appNameFromAnnotation;
        }

        // 2. Try to find the app name from a spring config file
        return APP_NAME_BY_ELEMENT_CACHE.computeIfAbsent(annotatedElement.hashCode(), key ->
                AppNameConfigReader.findSpringApplicationNameInClasspathResources(annotatedElement)
                        .orElseGet(() -> getSpringApplicationNameInConfigFiles(annotatedElement)));
    }

    private String getSpringApplicationNameInConfigFiles(Element annotatedElement) {
        Path annotatedSourceFile = sourceFiles.getAnnotatedSourceFile(annotatedElement);
        Path moduleDirectory = findModuleDirectory(annotatedSourceFile);
        // Module directory takes precedence over project root directory
        return APP_NAME_BY_MODULE_PATH_CACHE.computeIfAbsent(moduleDirectory, key ->
                AppNameConfigReader.findSpringApplicationNameInConfigFiles(moduleDirectory, annotatedElement)
                        .orElseGet(() -> findAppNameInProjectRoot(annotatedSourceFile, annotatedElement)));
    }

    private String findAppNameInProjectRoot(Path annotatedSourceFile, Element annotatedElement) {
        Path projectRootDirectory = findProjectRootDirectory(annotatedSourceFile);
        return APP_NAME_BY_PROJECT_PATH_CACHE.computeIfAbsent(projectRootDirectory, key -> {
            Optional<String> appName = AppNameConfigReader.findSpringApplicationNameInConfigFiles(projectRootDirectory, annotatedElement);
            return appName.orElseThrow(() -> MessagingAnnotationProcessingException.noAppNameFound(annotatedElement, projectRootDirectory));
        });
    }

    private Path findModuleDirectory(Path annotatedSourceFile) {
        return ProjectDirectoryLocator.findModuleDirectory(annotatedSourceFile)
                .orElseThrow(() -> MessagingAnnotationProcessingException.projectDirectoryNotFound(annotatedSourceFile));
    }

    private Path findProjectRootDirectory(Path annotatedSourceFile) {
        return ProjectDirectoryLocator.findProjectRootDirectory(annotatedSourceFile)
                .orElseThrow(() -> MessagingAnnotationProcessingException.projectDirectoryNotFound(annotatedSourceFile));
    }
}
