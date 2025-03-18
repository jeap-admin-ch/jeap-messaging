package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.errorevent.FailedMessageMetadata;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContextProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageRecorder {
    private final TraceContextProvider traceContextProvider;

    private final List<AvroMessage> recordedMessages = new ArrayList<>();
    @Getter
    private final List<Object> recordedErrorEvents = new ArrayList<>();
    private final Map<String, Long> traceContextIds = new HashMap<>();
    private final Map<String, AvroMessageKey> keyByMessageId = new HashMap<>();

    public void reset() {
        recordedMessages.clear();
        traceContextIds.clear();
        keyByMessageId.clear();
        recordedErrorEvents.clear();
    }

    synchronized void recordMessage(AvroMessage message) {
        log.info("Message {} handled in listener", message.getIdentity().getId());
        recordedMessages.add(message);
        traceContextIds.put(message.getIdentity().getId(), traceContextProvider.getTraceContext().getTraceId());
    }

    public void recordMessage(AvroMessageKey key, AvroMessage message) {
        recordMessage(message);
        keyByMessageId.put(message.getIdentity().getId(), key);
    }

    public MessageProcessingFailedEvent getExactlyOneMessageProcessingFailedEvent() {
        await().until(() -> recordedMessages.stream().anyMatch(MessageProcessingFailedEvent.class::isInstance));

        assertThat(recordedMessages.stream()
                .filter(MessageProcessingFailedEvent.class::isInstance)
                .count())
                .isEqualTo(1);

        return recordedMessages.stream()
                .filter(MessageProcessingFailedEvent.class::isInstance)
                .map(MessageProcessingFailedEvent.class::cast)
                .findFirst()
                .orElseThrow();
    }

    public void assertTraceContextForMessage(long expectedTraceId, AvroMessage message) {
        Long actualTraceId = traceContextIds.get(message.getIdentity().getId());

        assertThat(actualTraceId)
                .describedAs("Expected trace ID for message %s", message)
                .isEqualTo(expectedTraceId);
    }

    @TestKafkaListener(topics = "${jeap.messaging.kafka.error-topic-name}")
    public void onMessageProcessingFailedEvent(ConsumerRecord<?,?> consumerRecord) {
        if (consumerRecord.value() instanceof MessageProcessingFailedEvent message) {
            FailedMessageMetadata metadata = message.getPayload().getFailedMessageMetadata();
            log.info("MessageProcessingFailedEvent {} handled in listener", metadata == null ? "na" : metadata.getEventId());
            recordedMessages.add(message);
        } else {
            log.info("Event {} handled in listener", consumerRecord.value().getClass().getName());
            recordedErrorEvents.add(consumerRecord.value());
        }
    }

    public void assertMessageConsumed(String messageId) {
        await("Message with ID " + messageId + " was consumed")
                .until(() ->
                        recordedMessages.stream().anyMatch(e -> e.getIdentity().getId().equals(messageId)));
    }

    public void assertCountErrorEvents(int count) {
        await("Error Events Count is " + count)
                .until(() ->
                        recordedErrorEvents.size() == count);
    }

    public void assertMessageNotConsumed(String messageId) {
        assertThat(recordedMessages)
                .describedAs("Message with ID " + messageId + " was not consumed")
                .noneMatch(e -> e.getIdentity().getId().equals(messageId));
    }

    public void assertMessageProcessingFailedEventConsumedForMessage(AvroMessage message) {
        await().untilAsserted(() ->
                assertThat(recordedMessages)
                        .describedAs("Message " + message + " was sent in a MessageProcessingFailedEvent to the error topic")
                        .anyMatch(mpfe -> mpfeFor(mpfe, message)));
    }

    private boolean mpfeFor(AvroMessage maybeMpfe, AvroMessage avroMessage) {
        if (maybeMpfe instanceof MessageProcessingFailedEvent mpfe) {
            return mpfe.getPayload().getFailedMessageMetadata().getEventId().equals(avroMessage.getIdentity().getId());
        }
        return false;
    }

    public void assertKeyForMessage(AvroMessageKey expectedKey, AvroMessage message) {
        AvroMessageKey actualKey = keyByMessageId.get(message.getIdentity().getId());
        assertThat(actualKey).isEqualTo(expectedKey);
    }


    record SeqMsg(String messageType, String messageId) {
        public static SeqMsg of(AvroMessage msg) {
            return new SeqMsg(msg.getType().getName(), msg.getIdentity().getId());
        }
    }

    public void assertMessageSequenceForContextId(UUID contextId, AvroMessage... sequence) {
        List<SeqMsg> actualSequence = toSeq(contextId);
        List<SeqMsg> expectedSequence = Arrays.stream(sequence)
                .map(SeqMsg::of)
                .toList();

        assertThat(actualSequence)
                .describedAs("Expected message sequence for contextId %s", contextId)
                .isEqualTo(expectedSequence);
    }

    private List<SeqMsg> toSeq(UUID contextId) {
        return recordedMessages.stream()
                .filter(msg -> !(msg instanceof MessageProcessingFailedEvent))
                .filter(msg -> msg.getOptionalProcessId().orElseThrow().equals(contextId.toString()))
                .map(SeqMsg::of)
                .toList();
    }
}
