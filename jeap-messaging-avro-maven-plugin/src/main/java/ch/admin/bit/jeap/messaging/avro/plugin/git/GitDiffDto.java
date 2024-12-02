package ch.admin.bit.jeap.messaging.avro.plugin.git;

import java.util.Set;
import java.util.stream.Collectors;

public record GitDiffDto(
        Set<NewMessageTypeVersionDto> newMessageTypeVersions) {

    public Set<String> systems() {
        return newMessageTypeVersions.stream()
                .map(NewMessageTypeVersionDto::systemName)
                .collect(Collectors.toSet());
    }

    public boolean hasChanges() {
        return !newMessageTypeVersions.isEmpty();
    }
}
