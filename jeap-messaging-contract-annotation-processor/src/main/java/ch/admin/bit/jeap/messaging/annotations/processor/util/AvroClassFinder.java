package ch.admin.bit.jeap.messaging.annotations.processor.util;

import org.apache.avro.specific.AvroGenerated;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.vfs.Vfs;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AvroClassFinder {

    private final Messager messager;

    public AvroClassFinder(Messager messager) {
        this.messager = messager;
    }

    public Set<Class<?>> getAvroGeneratedClasses() {
        // Adding the custom UrlType to the default URL types ensures that Reflections can process the JAR files correctly.
        Vfs.addDefaultURLTypes(new CustomUrlType());

        // Load the JAR files from the (extended) classpath "java.class.path", to ensure that all JAR files in the classpath are searched, not just of this library.
        try (URLClassLoader extUrlClassLoader = ClasspathUtils.createExtendedClassLoader()) {

            // Filter paths to include only .jar files to avoid warnings and narrow the search
            URL[] urls = extUrlClassLoader.getURLs();
            List<URL> filteredUrls = new ArrayList<>();
            for (URL url : urls) {
                if (url.getPath().endsWith(".jar") || new File(url.getPath()).isDirectory()) {
                    filteredUrls.add(url);
                }
            }

            URLClassLoader urlClassLoader = new URLClassLoader(filteredUrls.toArray(new URL[0]), getClass().getClassLoader());

            // The annotation processor, who works during the compilation phase, does not touch already compiled Files.
            //Therefore @AvroGenerated classes must be searched using reflections because they are already compiled at the time of compilation.
            Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forClassLoader(urlClassLoader)).addScanners(Scanners.TypesAnnotated));

            return reflections.getTypesAnnotatedWith(AvroGenerated.class);
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Error loading classes: " + e.getMessage());
        }
        return Set.of();
    }
}
