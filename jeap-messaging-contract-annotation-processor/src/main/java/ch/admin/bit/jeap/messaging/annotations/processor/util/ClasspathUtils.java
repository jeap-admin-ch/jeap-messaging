package ch.admin.bit.jeap.messaging.annotations.processor.util;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class ClasspathUtils {
    public static URLClassLoader createExtendedClassLoader() throws Exception {
        String classpath = System.getProperty("java.class.path");
        String[] classpathEntries = classpath.split(File.pathSeparator);

        List<URL> urls = new ArrayList<>();
        for (String entry : classpathEntries) {
            File file = new File(entry);
            urls.add(file.toURI().toURL());
        }

        return new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }
}
