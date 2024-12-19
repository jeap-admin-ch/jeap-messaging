package ch.admin.bit.jeap.messaging.annotations.processor.util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.*;

class ClasspathUtilsTest {

    @Test
    void testUrlMatchesClasspath_and_parentClassLoaderIsTheContextPathLoader() throws Exception {
        URLClassLoader classLoader = ClasspathUtils.createExtendedClassLoader();

        String classpath = System.getProperty("java.class.path");
        String[] classpathEntries = classpath.split(File.pathSeparator);

        URL[] urls = classLoader.getURLs();
        assertEquals(classpathEntries.length, urls.length);

        for (int i = 0; i < classpathEntries.length; i++) {
            File file = new File(classpathEntries[i]);
            assertEquals(file.toURI().toURL(), urls[i]);
        }

        assertEquals(Thread.currentThread().getContextClassLoader(), classLoader.getParent());
    }
}
