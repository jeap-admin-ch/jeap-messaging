package ch.admin.bit.jeap.messaging.registry.helper;

import ch.admin.bit.jeap.messaging.registry.dto.CompatibilityMode;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

public class VersionCompatibility {

    private final Map<SemanticVersion, Compatibility> compatibilities;

    public record Compatibility(SemanticVersion newVersion, SemanticVersion oldVersion,
                                CompatibilityMode compatibilityMode) {
        public static Optional<Compatibility> parseIfDefined(JsonNode versionNode, SemanticVersionList semanticVersionList) {
            SemanticVersion newVersion = SemanticVersion.parse(versionNode.get("version").asText());
            Optional<CompatibilityMode> compatibilityModeIfPresent = Optional.ofNullable(versionNode.get("compatibilityMode"))
                    .map(JsonNode::asText)
                    .map(CompatibilityMode::valueOf);

            return compatibilityModeIfPresent.map(mode -> {
                JsonNode compatibleVersion = versionNode.get("compatibleVersion");
                SemanticVersion oldVersion = compatibleVersion == null ?
                        getPreviousVersion(semanticVersionList, newVersion) : SemanticVersion.parse(compatibleVersion.asText());
                return new Compatibility(newVersion, oldVersion, mode);
            });
        }

    }

    private static SemanticVersion getPreviousVersion(SemanticVersionList semanticVersionList, SemanticVersion newVersion) {
        List<SemanticVersion> versions = semanticVersionList.getVersions();
        int newVersionIndex = versions.indexOf(newVersion);
        boolean isFirstVersion = newVersionIndex == 0;
        return isFirstVersion ? null : versions.get(newVersionIndex - 1);
    }

    public VersionCompatibility(List<Compatibility> compatibilities) {
        this.compatibilities = compatibilities.stream()
                .collect(toMap(c -> c.newVersion, c -> c));
    }

    public static VersionCompatibility empty() {
        return new VersionCompatibility(List.of());
    }

    public Optional<Compatibility> getCompatibility(SemanticVersion fromVersion) {
        return Optional.ofNullable(compatibilities.get(fromVersion));
    }
}
