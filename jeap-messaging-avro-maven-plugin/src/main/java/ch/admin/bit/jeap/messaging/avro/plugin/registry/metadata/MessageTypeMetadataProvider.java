package ch.admin.bit.jeap.messaging.avro.plugin.registry.metadata;

import ch.admin.bit.jeap.messaging.avro.plugin.registry.TypeDescriptor;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.TypeVersion;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class MessageTypeMetadataProvider {
    public static MessageTypeMetadata createMessageTypeMetadata(TypeDescriptor typeDescriptor, TypeVersion version,
                                                                String currentBranch, String commitId, String registryUrl) {
        String compatibilityMode = version.getCompatibilityMode();
        if (compatibilityMode == null) {
            compatibilityMode = "NONE";
        }
        return MessageTypeMetadata.builder()
                .messageTypeVersion(version.getVersion())
                .messageTypeName(typeDescriptor.getName())
                .systemName(typeDescriptor.getDefiningSystem())
                .registryBranch(currentBranch)
                .registryCommit(commitId)
                .registryUrl(registryUrl)
                .compatibilityMode(compatibilityMode)
                .defaultTopic(typeDescriptor.getTopic())
                .topicNamesByConstantName(generateConstantNames(typeDescriptor.getAllTopics()))
                .build();
    }

    private static Map<String, String> generateConstantNames(Set<String> allTopicNames) {
        return allTopicNames.stream()
                .collect(Collectors.toMap(
                        MessageTypeMetadataProvider::generateConstantNameForTopicName,
                        topicName -> topicName,
                        MessageTypeMetadataProvider::throwingMerge,
                        TreeMap::new));
    }

    private static String throwingMerge(String a, String b) {
        throw new IllegalArgumentException("Duplicate key: " + a);
    }

    static String generateConstantNameForTopicName(String topicName) {
        String charactersNumbersAndUnderscoresOnly = topicName.toUpperCase().replaceAll("[^A-Z\\d]", "_");
        return "TOPIC_" + charactersNumbersAndUnderscoresOnly;
    }
}
