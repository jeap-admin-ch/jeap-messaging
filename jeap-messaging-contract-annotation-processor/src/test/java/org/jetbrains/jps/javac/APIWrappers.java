package org.jetbrains.jps.javac;

import javax.annotation.processing.ProcessingEnvironment;

/** Test double for the public JPS unwrap API. */
public final class APIWrappers {
    public interface Delegate {
        ProcessingEnvironment delegate();
    }

    private APIWrappers() { }

    public static <T> T unwrap(Class<T> type, T wrapper) {
        return wrapper instanceof Delegate delegate ? type.cast(delegate.delegate()) : wrapper;
    }
}
