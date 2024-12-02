package ch.admin.bit.jeap.messaging.annotations.processor.appname;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;

class SourceFiles {

    private static final String JETBRAINS_JPS_JAVAC_APIWRAPPERS = "org.jetbrains.jps.javac.APIWrappers";
    private static final String UNWRAP_METHOD = "unwrap";

    private final ProcessingEnvironment processingEnv;

    SourceFiles(ProcessingEnvironment processingEnv) {
        this.processingEnv = unwrapJpsProcessEnvIfRequired(ProcessingEnvironment.class, processingEnv);
    }

    Path getAnnotatedSourceFile(Element annotatedElement) {
        try {
            Trees trees = Trees.instance(processingEnv);
            TreePath path = trees.getPath(annotatedElement);
            URI uri = path.getCompilationUnit().getSourceFile().toUri();
            return Path.of(uri);
        } catch (Exception ignored) {
            return null;
        }
    }

    // Required to get process env in IntelliJ
    private static <T> T unwrapJpsProcessEnvIfRequired(Class<? extends T> iface, T wrapper) {
        T unwrapped = null;
        try {
            final Class<?> apiWrappers = wrapper.getClass().getClassLoader().loadClass(JETBRAINS_JPS_JAVAC_APIWRAPPERS);
            final Method unwrapMethod = apiWrappers.getDeclaredMethod(UNWRAP_METHOD, Class.class, Object.class);
            unwrapped = iface.cast(unwrapMethod.invoke(null, iface, wrapper));
        } catch (Throwable ignored) {
        }
        return unwrapped != null ? unwrapped : wrapper;
    }
}
