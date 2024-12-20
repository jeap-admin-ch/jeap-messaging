package ch.admin.bit.jeap.messaging.annotations.processor.util;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TemplatePathResolver {

    private final ProcessingEnvironment processingEnv;

    public TemplatePathResolver(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Since the template files of the process context service library are always located under classpath:/process/templates/*.json,
     * the path can be automatically determined via the annotated class.
     * As direct access to directories in the classpath is not supported at compile time, this was resolved through the StandardLocation.SOURCE_PATH
     * of the Maven module, where the annotated class is located.
     *
     * @param annotatedElement the annotated element from which the path is determined
     * @return the path to the templates directory as a String, or null if the path could not be determined
     */
    public String getTemplatePath(Element annotatedElement) {
        try {
            String className = ((TypeElement) annotatedElement).getQualifiedName().toString();
            FileObject fileObject = processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH, "", className.replace('.', '/') + ".java");

            String path = fileObject.toUri().getPath();

            Path sourcePath = Paths.get(path);

            while (sourcePath != null && !sourcePath.endsWith("src/main/java") && !sourcePath.endsWith("src/test/java")) {
                sourcePath = sourcePath.getParent();
            }

            if (sourcePath != null) {
                Path templatesPath;
                if (sourcePath.endsWith("src/main/java")) {
                    templatesPath = sourcePath.getParent().resolve("resources/process/templates");
                } else {
                    templatesPath = sourcePath.getParent().resolve("resources/process/templates");
                }
                return templatesPath.toAbsolutePath().toString();
            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "src/main/java or src/test/java directory not found.");
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }
}
