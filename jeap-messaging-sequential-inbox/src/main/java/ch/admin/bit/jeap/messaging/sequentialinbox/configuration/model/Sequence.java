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

    private Set<String> messageTypeQualifiedNames;

    @Override
    public String toString() {
        return "Sequence{name='%s',messages=%s}".formatted(name, messages);
    }

    public boolean isComplete(Set<String> processedMessageTypes) {
        return messageTypeQualifiedNames.equals(processedMessageTypes);
    }

    void init() {
        messageTypeQualifiedNames = messages.stream()
                .map(SequencedMessageType::getQualifiedName)
                .collect(toSet());
    }
}
