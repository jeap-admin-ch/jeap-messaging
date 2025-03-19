package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer.SequentialInboxConfigurationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
class ReleaseConditionsValidator {

    private final List<SequencedMessageType> messages;

    private Set<String> currentCheckedMessageTypes = new HashSet<>();
    private String currentMessageType;

    void validate() {

        messages.stream().filter(m -> m.getReleaseCondition() != null).forEach(message -> {
            log.info("Checking release conditions for message type: {}", message.getQualifiedName());
                    if (!message.getReleaseCondition().getAnd().isEmpty() && !message.getReleaseCondition().getOr().isEmpty()) {
                        throw SequentialInboxConfigurationException.invalidPredecessorConfiguration(message.getQualifiedName());
                    }

                    if (message.getReleaseCondition().getPredecessor() == null) {
                        if (message.getReleaseCondition().getAnd().isEmpty() && message.getReleaseCondition().getOr().isEmpty()) {
                            throw SequentialInboxConfigurationException.invalidPredecessorConfiguration(message.getQualifiedName());
                        }

                    } else {
                        if (!message.getReleaseCondition().getAnd().isEmpty() || !message.getReleaseCondition().getOr().isEmpty()) {
                            throw SequentialInboxConfigurationException.invalidPredecessorConfiguration(message.getQualifiedName());
                        }
                    }

                    currentCheckedMessageTypes = new HashSet<>();
            currentMessageType = message.getQualifiedName();
            validate(message.getQualifiedName(), message.getReleaseCondition());
                    log.info("Release conditions for {} are valid : {}", currentMessageType, currentCheckedMessageTypes.stream().sorted().toList());
                }

        );
    }

    private void validate(String parent, ReleaseCondition releaseCondition) {

        if (releaseCondition == null) {
            return;
        }

        if (releaseCondition.getPredecessor() != null && releaseCondition.getPredecessor().equals(currentMessageType)) {
            throw SequentialInboxConfigurationException.circularPredecessorDefinition(currentMessageType, parent);
        }

        if (releaseCondition.getPredecessor() != null) {

            SequencedMessageType predecessor = getPredecessorMessageType(releaseCondition.getPredecessor());

            if (predecessor.getQualifiedName().equals(currentMessageType)) {
                throw SequentialInboxConfigurationException.circularPredecessorDefinition(currentMessageType, parent);
            }

            if (!currentCheckedMessageTypes.add(predecessor.getQualifiedName())) {
                return;
            }

            validate(predecessor.getQualifiedName(), predecessor.getReleaseCondition());

        }

        if (releaseCondition.getAnd().size() == 1 || releaseCondition.getOr().size() == 1) {
            throw SequentialInboxConfigurationException.invalidPredecessorConfiguration(parent);
        }

        checkDuplicatesInOperation(parent, releaseCondition.getAnd());
        checkDuplicatesInOperation(parent, releaseCondition.getOr());

        releaseCondition.getAnd().forEach(current -> validate(parent, current));
        releaseCondition.getOr().forEach(current -> validate(parent, current));

    }

    private SequencedMessageType getPredecessorMessageType(String messageTypeQualifiedName) {
        return messages.stream().filter(m -> m.getQualifiedName().equals(messageTypeQualifiedName))
                .findFirst().orElseThrow(() -> SequentialInboxConfigurationException.predecessorNotFound(messageTypeQualifiedName));
    }

    private void checkDuplicatesInOperation(String parent, List<ReleaseCondition> releaseConditions) {
        List<ReleaseCondition> list = releaseConditions.stream().filter(current -> current.getPredecessor() != null).toList();
        if (list.size() != list.stream().map(ReleaseCondition::getPredecessor).distinct().count()) {
            throw SequentialInboxConfigurationException.duplicatedPredecessor(parent);
        }
    }
}
