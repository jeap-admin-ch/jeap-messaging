package ch.admin.bit.jeap.messaging.annotations.processor.util;

import javax.annotation.processing.Filer;
import javax.tools.StandardLocation;
import java.io.Closeable;
import java.io.IOException;

/** Loads the application's classes and dependencies, independently of the processor path. */
final class CompilerClasspathLoader extends ClassLoader implements Closeable {
    private Filer filer;

    CompilerClasspathLoader(Filer filer) {
        super(ClassLoader.getPlatformClassLoader());
        this.filer = filer;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        int separator = name.lastIndexOf('.');
        String packageName = separator < 0 ? "" : name.substring(0, separator);
        String fileName = name.substring(separator + 1) + ".class";
        try (var stream = filer.getResource(StandardLocation.CLASS_PATH, packageName, fileName).openInputStream()) {
            byte[] bytes = stream.readAllBytes();
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException | IllegalArgumentException ex) {
            throw new ClassNotFoundException(name + " on compiler CLASS_PATH", ex);
        }
    }

    @Override
    public void close() {
        filer = null;
    }
}
