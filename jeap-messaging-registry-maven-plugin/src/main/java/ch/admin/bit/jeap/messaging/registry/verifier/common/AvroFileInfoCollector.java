package ch.admin.bit.jeap.messaging.registry.verifier.common;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class AvroFileInfoCollector {

    private final Log log;

    Set<AvroFileInfo> collectAvroFileInfos(Set<String> files, ImportClassLoader importClassLoader) throws IOException {
        Set<AvroFileInfo> avroFileInfos = new HashSet<>();
        for (String file : files) {
            URL resource = importClassLoader.findResource(file);
            if (resource == null) {
                log.error("Resource not found: " + file + " in classloader: " + importClassLoader);
                throw new IllegalStateException("Resource not found: " + file);
            }
            log.debug("Found resource: " + resource + " for file: " + file + " with path " + resource.getPath() + " in classloader: " + importClassLoader);
            AvroFileInfo avroFileInfo = new AvroFileInfo(file);
            try (InputStream inputStream = getInputStream(resource)) {
                Set<AvroTypeInfo> avroTypeInfos = collectAvroTypeInfos(file, inputStream);
                avroTypeInfos.forEach(avroFileInfo::addDefinedType);
            } catch (IOException e) {
                throw new IOException("Error parsing imported file " + file + ": " + e.getMessage(), e);
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
        Set<AvroTypeInfo> types = IdlTokens.read(fileInputStream).declaredTypes();
        types.forEach(type -> log.debug("Found type " + type + " in " + filename));
        return types;
    }
}