package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@AllArgsConstructor // for builder
@NoArgsConstructor // for jackson
@Builder
public class SequencedMessageType {

    private String type;
    private String topic;
    private String clusterName;
    private ContextIdExtractor<AvroMessage> contextIdExtractor;
    private MessageFilter<AvroMessage> messageFilter;
    private ReleaseCondition releaseCondition;

    @Override
    public String toString() {
        return "SequencedMessageType{type='%s',topic=%s,clusterName=%s,contextIdExtractor=%s,messageFilter=%s,releaseCondition=%s}".formatted(type,
                topic,
                clusterName,
                contextIdExtractor.getClass().getName(),
                messageFilter != null ? messageFilter.getClass().getName() : "null",
                releaseCondition);
    }

    public String extractContextId(AvroMessage message) {
        return contextIdExtractor.extractContextId(message);
    }

    public boolean shouldSequenceMessage(AvroMessage value) {
        if (messageFilter == null) {
            return true;
        }
        return messageFilter.shouldSequence(value);
    }

    public boolean isReleaseConditionSatisfied(Set<String> previouslyReleasedMessageTypes) {
        return releaseCondition == null || releaseCondition.isSatisfied(previouslyReleasedMessageTypes);
    }
}
