package ch.admin.bit.jeap.messaging.avro.plugin.compiler;

import lombok.SneakyThrows;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ImportClassLoader extends URLClassLoader {
    private final Map<String, URL> importFiles;

    public ImportClassLoader() {
        super(new URL[0], Thread.currentThread().getContextClassLoader());
        importFiles = new HashMap<>();
    }

    public ImportClassLoader(File sourceDirectory) throws MalformedURLException {
        super(convertToUrlList(sourceDirectory, Collections.emptyList()), Thread.currentThread().getContextClassLoader());
        importFiles = new HashMap<>();
    }

    public ImportClassLoader(File sourceDirectory, List<String> classpathElements) throws MalformedURLException {
        super(convertToUrlList(sourceDirectory, classpathElements), Thread.currentThread().getContextClassLoader());
        importFiles = new HashMap<>();
    }

    public ImportClassLoader(ImportClassLoader importClassLoader, File commonRootDir, File commonSystemDir) {
        super(new URL[0], importClassLoader);
        importFiles = new HashMap<>();
        if (commonRootDir.exists()) {
            for (String filename : Objects.requireNonNull(commonRootDir.list())) {
                File file = new File(commonRootDir, filename);
                addImportFile(filename, file);
            }
        }
        if (commonSystemDir.exists()) {
            for (String filename : Objects.requireNonNull(commonSystemDir.list())) {
                File file = new File(commonSystemDir, filename);
                addImportFile(filename, file);
            }
        }
    }

    private static URL[] convertToUrlList(File sourceDirectory, List<String> classpathElements) throws MalformedURLException {
        List<URL> runtimeUrls = new ArrayList<>();
        if (sourceDirectory != null) {
            runtimeUrls.add(sourceDirectory.toURI().toURL());
        }
        for (String runtimeClasspathElement : classpathElements) {
            runtimeUrls.add(new File(runtimeClasspathElement).toURI().toURL());
        }
        return runtimeUrls.toArray(new URL[0]);
    }

    @SneakyThrows
    public void addImportFile(String filename, File file) {
        importFiles.put(filename, file.toURI().toURL());
    }


    @Override
    public URL findResource(String name) {
        if (importFiles.containsKey(name)) {
            return importFiles.get(name);
        }
        return super.findResource(name);
    }
}
