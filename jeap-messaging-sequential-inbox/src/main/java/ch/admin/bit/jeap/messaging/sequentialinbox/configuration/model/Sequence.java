package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import lombok.Getter;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Getter
public class Sequence {

    private String name;

    private Duration retentionPeriod;

    private List<SequencedMessageType> messages;

    private Set<String> messageTypeNames;

    @Override
    public String toString() {
        return "Sequence{name='%s',messages=%s}".formatted(name, messages);
    }

    public boolean isComplete(Set<String> processedMessageTypes) {
        return messageTypeNames.equals(processedMessageTypes);
    }

    void init() {
        messageTypeNames = messages.stream()
                .map(SequencedMessageType::getType)
                .collect(toSet());
    }
}
