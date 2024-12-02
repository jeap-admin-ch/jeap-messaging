package ch.admin.bit.jeap.messaging.avro.plugin.registry.metadata;

import lombok.Builder;
import lombok.Value;
import org.apache.avro.Schema;

import java.util.Map;
import java.util.TreeMap;

/**
 * Provides the necessary metadata to generate type reference blocks holding message type information.
 * Used in velocity template <pre>record.vm</pre>
 */
@SuppressWarnings("unused")
@Value
@Builder
public class MessageTypeMetadata {
    String messageTypeName;
    String messageTypeVersion;
    String systemName;
    String registryUrl;
    String registryBranch;
    String registryCommit;
    String compatibilityMode;
    Map<String, String> topicNamesByConstantName;
    String defaultTopic;

    public Map<String, String> getTopicNamesByConstantName() {
        if (topicNamesByConstantName == null) {
            return Map.of();
        }
        // Sort constants alphabetically
        return new TreeMap<>(topicNamesByConstantName);
    }

    public boolean shouldGenerateFor(Schema schema) {
        return messageTypeName.equals(schema.getName());
    }
}
