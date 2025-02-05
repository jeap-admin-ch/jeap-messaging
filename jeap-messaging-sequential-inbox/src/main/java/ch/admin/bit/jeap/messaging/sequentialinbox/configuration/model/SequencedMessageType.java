package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import lombok.Getter;

import java.time.Duration;

@Getter
public class SequencedMessageType {

    private String type;
    private String topic;
    private String clusterName;
    private Duration maxDelayPeriod;

    private ContextIdExtractor<?> contextIdExtractor;
    private MessageFilter<?> messageFilter;
    private ReleaseCondition releaseCondition;

    @Override
    public String toString() {
        return """
                SequencedMessageType{
                    type='%s',
                    topic=%s,
                    clusterName=%s,
                    maxDelayPeriod=%s,
                    contextIdExtractor=%s,
                    messageFilter=%s,
                    releaseCondition=%s
                }
                """.formatted(type,
                topic,
                clusterName,
                maxDelayPeriod,
                contextIdExtractor.getClass().getName(),
                messageFilter != null ? messageFilter.getClass().getName(): "null",
                releaseCondition);
    }

}
