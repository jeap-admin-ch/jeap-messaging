package ch.admin.bit.jeap.messaging.registry.verifier.common;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class AvroFileInfoCollector {

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("^@namespace\\(\"([^\"]+)\"\\)");
    private static final Pattern TYPE_PATTERN = Pattern.compile("\\b(record|enum|fixed)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\b");

    private final Log log;

    Set<AvroFileInfo> collectAvroFileInfos(Set<String> files, ImportClassLoader importClassLoader) {
        Set<AvroFileInfo> avroFileInfos = new HashSet<>();
        for (String file : files) {
            URL resource = importClassLoader.findResource(file);
            if (resource == null) {
                log.error("Resource not found: " + file + " in classloader: " + importClassLoader);
                throw new IllegalStateException("Resource not found: " + file);
            }
            log.debug("Found resource: " + resource + " for file: " + file + " with path " + resource.getPath() + " in classloader: " + importClassLoader);
            InputStream inputStream = getInputStream(resource);
            AvroFileInfo avroFileInfo = new AvroFileInfo(file);
            try {
                Set<AvroTypeInfo> avroTypeInfos = collectAvroTypeInfos(file, inputStream);
                avroTypeInfos.forEach(avroFileInfo::addDefinedType);
            } catch (IOException e) {
                throw new IllegalStateException("Error parsing imported file: " + file, e);
            }
            avroFileInfos.add(avroFileInfo);
        }
        return avroFileInfos;
    }

    private static InputStream getInputStream(URL resource) {
        try {
            return resource.openStream();
        } catch (IOException e) {
            throw new IllegalStateException("Could not open stream for " + resource, e);
        }
    }

    private Set<AvroTypeInfo> collectAvroTypeInfos(String filename, InputStream fileInputStream) throws IOException {
        Set<AvroTypeInfo> types = new HashSet<>();
        String namespace = ""; // namespace is not mandatory

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream))) {
            String line;

            while ((line = reader.readLine()) != null) {
                Matcher namespaceMatcher = NAMESPACE_PATTERN.matcher(line);
                if (namespaceMatcher.find()) {
                    namespace = namespaceMatcher.group(1);
                }

                Matcher typeMatcher = TYPE_PATTERN.matcher(line);
                if (typeMatcher.find()) {
                    AvroTypeInfo avroTypeInfo = new AvroTypeInfo(namespace, typeMatcher.group(2));
                    log.debug("Found type " + avroTypeInfo + " in " + filename);
                    types.add(avroTypeInfo);
                }
            }
        }
        return types;
    }
}