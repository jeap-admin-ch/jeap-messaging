package ch.admin.bit.jeap.messaging.annotations.processor.util;

import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import java.lang.reflect.InvocationTargetException;

/** JPS wraps the environment; unwrap only for Trees, never for tracked Filer access. */
public final class CompilerTrees {
    private CompilerTrees() {
    }

    public static Trees trees(ProcessingEnvironment environment) {
        ProcessingEnvironment unwrapped = environment;
        try {
            Class<?> wrappers = environment.getClass().getClassLoader().loadClass("org.jetbrains.jps.javac.APIWrappers");
            Object result = wrappers.getDeclaredMethod("unwrap", Class.class, Object.class)
                    .invoke(null, ProcessingEnvironment.class, environment);
            if (result instanceof ProcessingEnvironment delegate) {
                unwrapped = delegate;
            }
        } catch (InvocationTargetException ex) {
            rethrowFatal(ex.getCause());
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Not running under JPS.
        }
        return Trees.instance(unwrapped);
    }

    static void rethrowFatal(Throwable failure) {
        if (failure instanceof VirtualMachineError fatal) {
            throw fatal;
        }
        if (failure instanceof InvocationTargetException wrapped) {
            rethrowFatal(wrapped.getCause());
        }
    }
}
