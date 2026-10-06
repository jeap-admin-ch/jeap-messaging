package ch.admin.bit.jeap.messaging.annotations.processor.util;

import java.io.Closeable;
import java.io.IOException;

/**
 * Loads application classes independently of the processor path. Resources and service providers are
 * deliberately not exposed: schema extraction must not discover application logging backends or other
 * provider hooks. Generated schema JSON retains logical-type properties without loading custom providers.
 */
final class CompilerClasspathLoader extends ClassLoader implements Closeable {
    interface ClassBytes {
        byte[] read(String packageName, String fileName) throws IOException;
    }

    private volatile ClassBytes classBytes;

    CompilerClasspathLoader(ClassBytes classBytes) {
        super(ClassLoader.getPlatformClassLoader());
        this.classBytes = classBytes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        ClassBytes source = classBytes;
        if (source == null) {
            throw new ClassNotFoundException(name + " (schema extraction loader closed)");
        }
        int separator = name.lastIndexOf('.');
        String packageName = separator < 0 ? "" : name.substring(0, separator);
        String fileName = name.substring(separator + 1) + ".class";
        try {
            byte[] bytes = source.read(packageName, fileName);
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
