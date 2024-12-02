package ch.admin.bit.jeap.messaging.registry.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class ValidatorUtils {
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

    public static Optional<JsonNode> loadOldDescriptorIfExists(ValidationContext validationContext) throws IOException {
        String absoluteFile = validationContext.getDescriptorFile().getAbsolutePath();
        String absoluteDescriptorDir = validationContext.getDescriptorDir().getAbsolutePath();
        String relativeDescriptorFile = absoluteFile.replace(absoluteDescriptorDir, "");
        File oldDescriptorFile = new File(validationContext.getOldDescriptorDir(), relativeDescriptorFile);
        return oldDescriptorFile.exists() ? Optional.of(JsonLoader.fromFile(oldDescriptorFile)) : Optional.empty();
    }
}
