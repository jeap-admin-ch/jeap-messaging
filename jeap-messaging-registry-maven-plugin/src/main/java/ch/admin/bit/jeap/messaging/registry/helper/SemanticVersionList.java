package ch.admin.bit.jeap.messaging.registry.helper;


import java.util.List;

public class SemanticVersionList {
    private final List<SemanticVersion> versions;

    public static SemanticVersionList from(List<String> versionStrings) {
        return new SemanticVersionList(versionStrings.stream()
                .map(SemanticVersion::parse)
                .sorted()
                .toList());
    }

    public SemanticVersionList(List<SemanticVersion> versions) {
        this.versions = versions.stream()
                .sorted()
                .toList();
    }

    public static SemanticVersionList empty() {
        return new SemanticVersionList(List.of());
    }

    public List<SemanticVersion> getVersions() {
        return versions;
    }
}
