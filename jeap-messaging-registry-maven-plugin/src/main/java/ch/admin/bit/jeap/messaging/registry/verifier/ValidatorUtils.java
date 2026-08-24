package ch.admin.bit.jeap.messaging.registry.verifier;

import com.networknt.schema.Error;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.util.Optional;

public class ValidatorUtils {
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    /**
     * System name in descriptor for shared messages
     */
    private static final String SHARED_SYSTEM = "Shared";
    private static final String SHARED_WITH_PREFIX = "_" + SHARED_SYSTEM;

    public static final String JEAP_SYSTEM_NAME = "jeap";

    public static String getSystemNamePrefix(String systemName) {
        String systemNameCamelCase;
        if (SHARED_WITH_PREFIX.equalsIgnoreCase(systemName)) {
            systemNameCamelCase = SHARED_SYSTEM;
        } else {
            systemNameCamelCase = StringUtils.capitalize(systemName);
        }
        return systemNameCamelCase;
    }

    public static JsonNode loadJson(File file) throws JacksonException {
        return JSON_MAPPER.readTree(file);
    }

    public static String formatSchemaError(Error error) {
        if ("additionalProperties".equals(error.getKeyword()) && error.getProperty() != null) {
            return "object instance has properties which are not allowed by the schema: [\"" + error.getProperty() + "\"]";
        }
        return error.toString();
    }

    public static Optional<JsonNode> loadOldDescriptorIfExists(ValidationContext validationContext) throws JacksonException {
        String absoluteFile = validationContext.getDescriptorFile().getAbsolutePath();
        String absoluteDescriptorDir = validationContext.getDescriptorDir().getAbsolutePath();
        String relativeDescriptorFile = absoluteFile.replace(absoluteDescriptorDir, "");
        File oldDescriptorFile = new File(validationContext.getOldDescriptorDir(), relativeDescriptorFile);
        return oldDescriptorFile.exists() ? Optional.of(loadJson(oldDescriptorFile)) : Optional.empty();
    }
}
