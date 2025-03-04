package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer.SequentialInboxConfigurationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.*;

import static java.util.stream.Collectors.toSet;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
public class SequentialInboxConfiguration {

    @JsonProperty("sequences")
    private List<Sequence> sequences;

    private Map<String, Sequence> sequenceByMessageTypeName;
    private Map<String, SequencedMessageType> sequencedMessageTypeByName;

    public void validateAndInitialize() {
        checkDuplicatedMessageTypesAndTopics();
        checkRetentionPeriod();

        sequenceByMessageTypeName = new HashMap<>();
        sequencedMessageTypeByName = new HashMap<>();
        for (Sequence sequence : sequences) {
            validateSequence(sequence);
            sequence.init();
            for (SequencedMessageType message : sequence.getMessages()) {
                sequenceByMessageTypeName.put(message.getType(), sequence);
                sequencedMessageTypeByName.put(message.getType(), message);
            }
        }
    }

    private void checkRetentionPeriod() {
        sequences.forEach(sequence -> {
            if (sequence.getRetentionPeriod() == null || sequence.getRetentionPeriod().toMinutes() == 0) {
                throw SequentialInboxConfigurationException.retentionPeriodMissing(sequence.getName());
            }
        });
    }

    public Sequence getSequenceByMessageTypeName(String messageTypeName) {
        return sequenceByMessageTypeName.get(messageTypeName);
    }

    public SequencedMessageType requireSequencedMessageTypeByName(String messageTypeName) {
        SequencedMessageType sequencedMessageType = sequencedMessageTypeByName.get(messageTypeName);
        if (sequencedMessageType == null) {
            throw SequentialInboxConfigurationException.messageTypeNotConfiguredInAnySequence(messageTypeName);
        }
        return sequencedMessageType;
    }

    private static void validateSequence(Sequence sequence) {
        if (!hasText(sequence.getName())) {
            throw SequentialInboxConfigurationException.missingSequenceName();
        }
        if (sequence.getMessages() == null || sequence.getMessages().isEmpty()) {
            throw SequentialInboxConfigurationException.emptySequence(sequence.getName());
        }

        sequence.getMessages().forEach(message ->
                validateSequencedMessageType(sequence.getName(), message));

        ReleaseConditionsValidator validator = new ReleaseConditionsValidator(sequence.getMessages());
        validator.validate();
    }

    private static void validateSequencedMessageType(String sequenceName, SequencedMessageType sequencedMessageType) {
        if (!StringUtils.hasText(sequencedMessageType.getType())) {
            throw SequentialInboxConfigurationException.missingMessageType(sequenceName);
        }
        if (sequencedMessageType.getContextIdExtractor() == null) {
            throw SequentialInboxConfigurationException.missingContextIdExtractor(sequenceName);
        }
    }

    private void checkDuplicatedMessageTypesAndTopics() {
        log.debug("Checking duplicated message types and topics");
        Set<String> messageTypes = new HashSet<>();
        Set<String> duplicatedMessageTypes = new HashSet<>();
        Set<String> topics = new HashSet<>();
        Set<String> duplicatedTopics = new HashSet<>();

        sequences.forEach(sequence -> sequence.getMessages().forEach(message -> {
            String type = message.getType();
            String topic = message.getTopic();

            if (!messageTypes.add(type)) {
                duplicatedMessageTypes.add(type);
            }

            if (topic != null && !topics.add(topic)) {
                duplicatedTopics.add(topic);
            }
        }));

        if (!duplicatedMessageTypes.isEmpty()) {
            throw SequentialInboxConfigurationException.duplicatedMessageTypes(duplicatedMessageTypes);
        }

        if (!duplicatedTopics.isEmpty()) {
            throw SequentialInboxConfigurationException.duplicatedTopics(duplicatedTopics);
        }
    }

    public Set<SequencedMessageType> getSequencedMessageTypes() {
        return sequences.stream()
                .flatMap(sequence -> sequence.getMessages().stream())
                .collect(toSet());
    }

    public int getSequenceCount() {
        return sequences.size();
    }

    @Override
    public String toString() {
        return sequences.toString();
    }
}
