package ch.admin.bit.jeap.messaging.annotations.processor.appname;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

class ConfigParser {

    private static final String PROPERTIES = ".properties";

    static String getSpringApplicationName(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return getSpringApplicationName(path.toString(), inputStream);
        } catch (IOException e) {
            // Invalid config file path - ignore
            return null;
        }
    }

    static String getSpringApplicationName(String fileName, InputStream inputStream) {
        try {
            if (fileName.endsWith(PROPERTIES)) {
                return getSpringApplicationNameFromProperties(inputStream);
            } else {
                return getSpringApplicationNameFromYaml(inputStream);
            }
        } catch (Exception e) {
            // Invalid config file format - ignore
            return null;
        }
    }

    private static String getSpringApplicationNameFromYaml(InputStream inputStream) {
        Map<Object, Object> data = loadYaml(inputStream);
        String appName = getAppNameFromYamlMap(data);
        return emptyToNull(appName);
    }

    private static Map<Object, Object> loadYaml(InputStream inputStream) {
        Yaml yml = new Yaml();
        return yml.load(inputStream);
    }

    private static String getAppNameFromYamlMap(Map<Object, Object> data) {
        return getPossiblyDottedKey(data, "spring", List.of("application", "name"));
    }

    private static String getPossiblyDottedKey(Map<Object, Object> map, String key, List<String> subkeys) {
        if (subkeys.isEmpty()) {
            return (String) map.get(key);
        }
        if (map.containsKey(key)) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> subMap = (Map<Object, Object>) map.get(key);
            return getPossiblyDottedKey(subMap, subkeys.get(0), subkeys.subList(1, subkeys.size()));
        }
        return getPossiblyDottedKey(map, key + "." + subkeys.get(0), subkeys.subList(1, subkeys.size()));
    }

    private static String getSpringApplicationNameFromProperties(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        String appName = properties.getProperty("spring.application.name");
        return emptyToNull(appName);
    }

    private static String emptyToNull(String appName) {
        return appName != null && !appName.isBlank() ? appName : null;
    }
}
