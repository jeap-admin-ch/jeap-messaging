package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import lombok.*;

import java.util.Set;

@AllArgsConstructor // for builder
@NoArgsConstructor // for jackson
@Builder
public class SequencedMessageType {

    private String type;
    @Getter(AccessLevel.PACKAGE)
    private String subType;
    @Getter
    private String topic;
    @Getter
    private String clusterName;
    @Getter
    private ContextIdExtractor<AvroMessage> contextIdExtractor;
    @Getter
    private MessageFilter<AvroMessage> messageFilter;
    @Getter
    private ReleaseCondition releaseCondition;

    /**
     * @return The message type name, postfixed with .&lt;subType&gt; if a subtype is configured
     */
    public String getQualifiedName() {
        if (subType == null) {
            return type;
        }
        return type + "." + subType;
    }

    /**
     * @return The jEAP message type name for this sequenced message type, without any subtype postfix
     */
    public String getJeapMessageTypeName() {
        return type;
    }

    @Override
    public String toString() {
        return "SequencedMessageType{type='%s',subType='%s',topic=%s,clusterName=%s,contextIdExtractor=%s,messageFilter=%s,releaseCondition=%s}".formatted(type,
                subType,
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
