package ch.admin.bit.jeap.messaging.annotations.processor.appname;

import ch.admin.bit.jeap.messaging.annotations.processor.util.CompilerTrees;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.net.URI;
import java.nio.file.Path;

class SourceFiles {

    private final ProcessingEnvironment processingEnv;

    SourceFiles(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    Path getAnnotatedSourceFile(Element annotatedElement) {
        try {
            Trees trees = CompilerTrees.trees(processingEnv);
            TreePath path = trees.getPath(annotatedElement);
            URI uri = path.getCompilationUnit().getSourceFile().toUri();
            return Path.of(uri);
        } catch (Exception ignored) {
            return null;
        }
    }

}
