package ch.admin.bit.jeap.messaging.annotations.processor.util;

import java.io.Closeable;
import java.io.IOException;

/** Loads the application's classes and dependencies, independently of the processor path. */
final class CompilerClasspathLoader extends ClassLoader implements Closeable {
    interface ClassBytes {
        byte[] read(String packageName, String fileName) throws IOException;
    }

    private ClassBytes classBytes;

    CompilerClasspathLoader(ClassBytes classBytes) {
        super(ClassLoader.getPlatformClassLoader());
        this.classBytes = classBytes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        int separator = name.lastIndexOf('.');
        String packageName = separator < 0 ? "" : name.substring(0, separator);
        String fileName = name.substring(separator + 1) + ".class";
        try {
            byte[] bytes = classBytes.read(packageName, fileName);
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException | IllegalArgumentException ex) {
            throw new ClassNotFoundException(name + " on compiler CLASS_PATH", ex);
        }
    }

    @Override
    public void close() {
        classBytes = null;
    }
}
