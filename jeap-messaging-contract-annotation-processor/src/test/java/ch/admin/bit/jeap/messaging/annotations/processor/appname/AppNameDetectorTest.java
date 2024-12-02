package ch.admin.bit.jeap.messaging.annotations.processor.appname;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class AppNameDetectorTest {

    private Path projectRoot;
    private SourceFiles sourceFiles;
    private AppNameDetector appNameDetector;

    @Test
    void detectAppName_singleModuleProject() throws IOException {
        Path sourceFile = createAnnotatedSourceFileIn(projectRoot);
        createFile(projectRoot.resolve("pom.xml"));
        createFile(projectRoot.resolve("src/main/resources/application.yaml"), "spring.application.name: the-app");
        Element annotatedElement = mock(Element.class);
        String noAppNameFromAnnotation = null;

        String appName = appNameDetector.detectAppName(annotatedElement, noAppNameFromAnnotation);

        assertThat(appName).isEqualTo("the-app");
    }

    @Test
    void detectAppName_multiModuleProject() throws IOException {
        createFile(projectRoot.resolve("pom.xml"));
        createFile(projectRoot.resolve("module-1/pom.xml"));
        Path sourceFile = createAnnotatedSourceFileIn(projectRoot.resolve("module-1"));
        createFile(projectRoot.resolve("module-2/pom.xml"));
        createFile(projectRoot.resolve("module-2/src/main/resources/application.yaml"), "spring.application.name: the-app");
        Element annotatedElement = mock(Element.class);
        String noAppNameFromAnnotation = null;

        String appName = appNameDetector.detectAppName(annotatedElement, noAppNameFromAnnotation);

        assertThat(appName).isEqualTo("the-app");
    }

    @Test
    void detectAppName_nameFromAnnotationTakesPrecedence() throws IOException {
        createFile(projectRoot.resolve("pom.xml"));
        createFile(projectRoot.resolve("src/main/resources/application.yaml"), "spring.application.name: the-app");
        Path sourceFile = createAnnotatedSourceFileIn(projectRoot);
        Element annotatedElement = mock(Element.class);
        String appNameFromAnnotation = "more-important-app";

        String appName = appNameDetector.detectAppName(annotatedElement, appNameFromAnnotation);

        assertThat(appName).isEqualTo(appNameFromAnnotation);
    }

    @Test
    void detectAppName_appNameNotFound() throws IOException {
        createFile(projectRoot.resolve("pom.xml"));
        Path sourceFile = createAnnotatedSourceFileIn(projectRoot);
        Element annotatedElement = mock(Element.class);
        String noAppNameFromAnnotation = null;

        assertThatThrownBy(() -> appNameDetector.detectAppName(annotatedElement, noAppNameFromAnnotation))
                .hasMessageContaining("No application name found")
                .hasMessageContaining("Project root: /project");
    }

    @Test
    void detectAppName_projectRootNotFound() throws IOException {
        Path sourceFile = createAnnotatedSourceFileIn(projectRoot);
        Element annotatedElement = mock(Element.class);
        String noAppNameFromAnnotation = null;

        assertThatThrownBy(() -> appNameDetector.detectAppName(annotatedElement, noAppNameFromAnnotation))
                .hasMessageContaining("No application name found")
                .hasMessageContaining("a pom.xml file must be present");
    }

    @Test
    void detectAppName_ambiguousName() throws IOException {
        Path sourceFile = createAnnotatedSourceFileIn(projectRoot);
        createFile(projectRoot.resolve("pom.xml"));
        createFile(projectRoot.resolve("src/main/resources/application.yaml"), "spring.application.name: the-app");
        createFile(projectRoot.resolve("src/main/resources/bootstrap.yaml"), "spring.application.name: another-app");
        Element annotatedElement = mock(Element.class);
        String noAppNameFromAnnotation = null;

        assertThatThrownBy(() -> appNameDetector.detectAppName(annotatedElement, noAppNameFromAnnotation))
                .hasMessageContaining("Multiple different application names");
    }

    @BeforeEach
    void setUp() {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        projectRoot = fileSystem.getPath("/project");

        sourceFiles = mock(SourceFiles.class);
        appNameDetector = new AppNameDetector(sourceFiles);
    }

    private Path createAnnotatedSourceFileIn(Path directory) throws IOException {
        Path sourceFile = directory.resolve("src/main/java/test/MyFile.java");
        createFile(sourceFile);
        doReturn(sourceFile).when(sourceFiles).getAnnotatedSourceFile(any());
        return sourceFile;
    }

    private static void createFile(Path path) throws IOException {
        createFile(path, "dummyContent");
    }

    private static void createFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
