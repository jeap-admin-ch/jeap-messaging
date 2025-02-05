package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer.SequentialInboxConfigurationException;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@ToString
@Getter
@Slf4j
public class SequentialInboxConfiguration {

    private List<Sequence> sequences;

    public void validate() {
        checkDuplicatedMessageTypesAndTopics();
        sequences.forEach(sequence ->
                (new ReleaseConditionsValidator(sequence.getMessages()))
                        .validate()
        );
    }

    private void checkDuplicatedMessageTypesAndTopics() {
        log.info("Checking duplicated message types and topics");
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

}
