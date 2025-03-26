package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.DeclarationCreatedEventListener;
import ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.MultipleTestEventListener;
import ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.BeanReferenceMessageKey;
import ch.admin.bit.jme.test.JmeEnumTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.createDeclarationCreatedEvent;
import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.createEnumTestEvent;
import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.createJmeSimpleTestEvent;
import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.randomContextId;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@TestPropertySource(properties = "jeap.messaging.kafka.expose-message-key-to-consumer=true")
class SequentialInboxIT extends SequentialInboxITBase {

    @Test
    void testInbox_messageWithoutPredecessor_processedSuccessfully() {
        // given: a test event
        UUID contextId = randomContextId();
        JmeDeclarationCreatedEvent event = createDeclarationCreatedEvent(contextId);

        // when: sending the event
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event);

        // then: assert that the event was consumed by the message listener
        assertMessageConsumedByListener(event);
        assertSequencedMessageProcessedSuccessfully(event);
        assertSequenceOpen(event);
        assertBufferedMessageCount(contextId, 0);
    }

    @Test
    void testInbox_messageWithPredecessor_waitingAndBuffered() {
        // given: a test event with a predecessor
        UUID contextId = randomContextId();
        JmeSimpleTestEvent event = createJmeSimpleTestEvent(contextId);

        // when: sending the event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event);

        // then: assert that the event was buffered and not consumed by the message listener
        assertMessageCountHandledByInbox(1);
        assertMessageNotConsumedByListener(event);
        assertMessageStateWaitingAndBuffered(event);
        assertSequenceOpen(event);
        assertBufferedMessageCount(contextId, 1);
    }

    @Test
    void testInbox_message_notSequenced() {
        // given: a test event with a message for which TestMessageFilter.shouldSequence returns false
        JmeDeclarationCreatedEvent event = createDeclarationCreatedEvent("NOT_SEQUENCED");

        // when: sending the event
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event);

        // then: assert that the event was consumed by the message listener and has not been persisted
        assertMessageConsumedByListener(event);
        assertMessageCountHandledByInbox(1);
        assertNoSequencePersistedForContextOfEvent(event);
    }

    @Test
    void testInbox_idempotentEventProcessedOnlyOnce() {
        // given: two events with the same idempotence ID
        UUID idempotenceId = UUID.randomUUID();
        UUID contextId = UUID.randomUUID();
        JmeDeclarationCreatedEvent event1 = createDeclarationCreatedEvent(idempotenceId, contextId);
        JmeDeclarationCreatedEvent event2 = createDeclarationCreatedEvent(idempotenceId, contextId);

        // when: sending first even
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event1);

        // then: assert that the first event was consumed
        assertSequencedMessageProcessedSuccessfully(event1);
        assertMessageConsumedByListener(event1);

        // when: sending the second event with the same idempotence ID
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event2);
        assertMessageCountHandledByInbox(2);

        // then: assert that the second event was not processed by the message listener
        assertMessageNotConsumedByListener(event2);
        assertSequenceOfMessages(contextId, event1);
        assertSequenceOpen(contextId);
    }

    @Test
    void testInbox_messageWithPredecessor_bufferedAndThenProcessedAfterPredecessorHandled() {
        // given: an event with a predecessor
        UUID contextId = randomContextId();
        JmeSimpleTestEvent successorEvent = createJmeSimpleTestEvent(contextId);

        // when: sending the successor event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, successorEvent);

        // then: assert that the event was buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(1);
        assertMessageNotConsumedByListener(successorEvent);
        assertMessageStateWaitingAndBuffered(successorEvent);

        // when: sending the predecessor event for the same context ID
        JmeDeclarationCreatedEvent predecessorEvent = createDeclarationCreatedEvent(contextId);
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, predecessorEvent);

        // then: assert that the predecessor event was consumed by the message listener
        assertMessageCountHandledByInbox(2);
        assertMessageConsumedByListener(predecessorEvent);
        assertSequencedMessageProcessedSuccessfully(predecessorEvent);

        // then: assert that the successor event was consumed by the message listener
        assertMessageConsumedByListener(successorEvent);
        assertSequencedMessageProcessedSuccessfully(successorEvent);
        assertSequenceOfMessages(contextId, predecessorEvent, successorEvent);
        assertSequenceClosed(contextId);
    }


    @Test
    void testInbox_messageWithPredecessor_bufferedAndThenProcessedAfterPredecessorHandled_metricsProvided() {
        // given: an event with a predecessor
        UUID contextId = randomContextId();
        JmeSimpleTestEvent successorEvent = createJmeSimpleTestEvent(contextId);

        // when: sending the successor event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, successorEvent);

        // then: assert that the event was buffered and not yet consumed by the message listener
        assertMessageStateWaitingAndBuffered(successorEvent);

        // when: sending the predecessor event for the same context ID
        JmeDeclarationCreatedEvent predecessorEvent = createDeclarationCreatedEvent(contextId);
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, predecessorEvent);

        // then: assert that the predecessor event was consumed by the message listener
        assertSequencedMessageProcessedSuccessfully(predecessorEvent);

        // then: assert that the successor event was consumed by the message listener
        assertSequencedMessageProcessedSuccessfully(successorEvent);
        assertSequenceClosed(contextId);

        // then: assert metric values
        Optional<Counter> counterOptional = findMeter(Counter.class,
                "jeap.messaging.sequential-inbox.consumed-messages", "JmeSimpleTestEvent");
        assertThat(counterOptional)
                .isPresent()
                .hasValueSatisfying(counter ->
                        assertThat(counter.count())
                                .isPositive());
        Optional<Timer> timerOptional = findMeter(Timer.class,
                "jeap.messaging.sequential-inbox.waiting-message-delay", "JmeSimpleTestEvent");
        assertThat(timerOptional)
                .isPresent()
                .hasValueSatisfying(timer ->
                        assertThat(timer.count())
                                .isPositive());
        Optional<Gauge> gaugeOptional = findMeter(Gauge.class,
                "jeap.messaging.sequential-inbox.waiting-messages", "JmeSimpleTestEvent");
        assertThat(gaugeOptional)
                .isPresent()
                .hasValueSatisfying(gauge ->
                        assertThat(gauge.value())
                                .isGreaterThanOrEqualTo(0));
    }

    private <T> Optional<T> findMeter(Class<T> meterClass, String meterName, String messageType) {
        return meterRegistry.getMeters().stream()
                .filter(meter -> meterName.equals(meter.getId().getName()))
                .filter(meter -> messageType.equals(meter.getId().getTag("type")))
                .map(meterClass::cast)
                .findFirst();
    }

    @Test
    void testInbox_messageWithPredecessor_bufferedThenProcessed_assertTraceContextId() {
        // given: an event with a predecessor
        UUID contextId = randomContextId();
        JmeSimpleTestEvent successorEvent = createJmeSimpleTestEvent(contextId);

        // when: sending the successor event
        setTraceContext(123L);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, successorEvent);

        // then: assert that the event was buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(1);
        assertMessageNotConsumedByListener(successorEvent);
        assertMessageStateWaitingAndBuffered(successorEvent);

        // when: sending the predecessor event for the same context ID
        JmeDeclarationCreatedEvent predecessorEvent = createDeclarationCreatedEvent(contextId);
        setTraceContext(456L);
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, predecessorEvent);

        // then: assert that the predecessor event was consumed by the message listener
        assertMessageCountHandledByInbox(2);
        assertMessageConsumedByListener(predecessorEvent);
        assertSequencedMessageProcessedSuccessfully(predecessorEvent);

        // then: assert that the successor event was consumed by the message listener
        assertMessageConsumedByListener(successorEvent);
        assertSequencedMessageProcessedSuccessfully(successorEvent);
        assertSequenceOfMessages(contextId, predecessorEvent, successorEvent);
        assertSequenceClosed(contextId);

        // then: assert that the trace context ID of the original message was set in the listener
        assertTraceContextId(456L, predecessorEvent);
        assertTraceContextId(123L, successorEvent);
    }

    @Test
    void testInbox_messageWithPredecessor_withKeys_bufferedAndThenProcessedAfterPredecessorHandled() {
        // given: an event with a predecessor
        UUID contextId = randomContextId();
        JmeSimpleTestEvent successorEvent = createJmeSimpleTestEvent(contextId);

        // when: sending the successor event
        AvroMessageKey successorKey = createKey();
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, successorKey, successorEvent);

        // then: assert that the event was buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(1);
        assertMessageNotConsumedByListener(successorEvent);
        assertMessageStateWaitingAndBuffered(successorEvent);

        // when: sending the predecessor event for the same context ID
        JmeDeclarationCreatedEvent predecessorEvent = createDeclarationCreatedEvent(contextId);
        AvroMessageKey predecessorKey = createKey();
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, predecessorKey, predecessorEvent);

        // then: assert that the predecessor event was consumed by the message listener
        assertMessageCountHandledByInbox(2);
        assertMessageConsumedByListener(predecessorEvent);
        assertSequencedMessageProcessedSuccessfully(predecessorEvent);

        // then: assert that the successor event was consumed by the message listener
        assertMessageConsumedByListener(successorEvent);
        assertSequencedMessageProcessedSuccessfully(successorEvent);
        assertSequenceOfMessages(contextId, predecessorEvent, successorEvent);
        assertSequenceClosed(contextId);

        // then: assert that the keys have been passed to the listener for both the buffered and the immediately processed message
        assertKeyConsumedByListener(predecessorKey, predecessorEvent);
        assertKeyConsumedByListener(successorKey, successorEvent);
    }

    private static AvroMessageKey createKey() {
        return BeanReferenceMessageKey.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setName("test")
                .setNamespace("test").build();
    }

    @Test
    void testInbox_messageWithPredecessor_bufferedAndThenProcessedAfterPredecessorHandled_failFirstThenRetry() {
        // given: an event with a predecessor that will fail processing
        UUID contextId = randomContextId();
        JmeSimpleTestEvent successorEvent = createJmeSimpleTestEvent(contextId);

        // when: sending the successor event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, successorEvent);

        // then: assert that the event was buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(1);
        assertMessageNotConsumedByListener(successorEvent);
        assertMessageStateWaitingAndBuffered(successorEvent);

        // when: sending the predecessor event for the same context ID, and provoking a failure for the successor event
        MultipleTestEventListener.failOnJmeSimpleTestEvent = true;
        JmeDeclarationCreatedEvent predecessorEvent = createDeclarationCreatedEvent(contextId);
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, predecessorEvent);

        // then: assert that the predecessor event was consumed by the message listener
        assertMessageCountHandledByInbox(2);
        assertMessageConsumedByListener(predecessorEvent);
        assertSequencedMessageProcessedSuccessfully(predecessorEvent);

        // then: assert that the successor event was marked as failed and sent to the EHS
        assertMessageSentToErrorHandlingService(successorEvent);
        assertMessageStateFailed(successorEvent);
        assertSequenceOpen(contextId);

        // when: simulating a retry by the EHS by sending the successor event again
        MultipleTestEventListener.failOnJmeSimpleTestEvent = false;
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, successorEvent);

        // then: assert that the successor event was consumed by the message listener
        assertMessageCountHandledByInbox(3);
        assertMessageConsumedByListener(successorEvent);
        assertSequencedMessageProcessedSuccessfully(successorEvent);
        assertSequenceClosed(contextId);
        assertSequencedMessageCount(contextId, 2);
        assertBufferedMessageCount(contextId, 1);
    }

    @Test
    void testInbox_messageWithPredecessor_handlingOfWaitingMessagesIsIdempotent() {
        // given: an event (successor) with a predecessor
        UUID contextId = randomContextId();
        JmeSimpleTestEvent successorEvent = createJmeSimpleTestEvent(contextId);

        // when: sending the successor event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, successorEvent);

        // then: assert that the event was buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(1);
        assertMessageNotConsumedByListener(successorEvent);
        assertMessageStateWaitingAndBuffered(successorEvent);

        // when: sending the successor event again
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, successorEvent);

        // then:
        assertMessageCountHandledByInbox(2);
        assertMessageNotConsumedByListener(successorEvent);

        assertSequencedMessageCount(contextId, 1);
        assertBufferedMessageCount(contextId, 1);
    }

    @Test
    void testInbox_messageWithPredecessor_sameMessageTypeTwiceBuffered_thenProcessedAfterPredecessor() {
        // given: an event with a predecessor
        UUID contextId = randomContextId();
        JmeSimpleTestEvent successorEvent1 = createJmeSimpleTestEvent(contextId);
        JmeSimpleTestEvent successorEvent2 = createJmeSimpleTestEvent(contextId);

        // when: sending the successor event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, successorEvent1);

        // then: assert that the event was buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(1);
        assertMessageNotConsumedByListener(successorEvent1);
        assertMessageStateWaitingAndBuffered(successorEvent1);

        // when: sending the successor event type again, with a different ID
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, successorEvent2);

        // then: assert that the event was buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(2);
        assertMessageNotConsumedByListener(successorEvent2);
        assertMessageStateWaitingAndBuffered(successorEvent2);

        // when: sending the predecessor event for the same context ID
        JmeDeclarationCreatedEvent predecessorEvent = createDeclarationCreatedEvent(contextId);
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, predecessorEvent);

        // then: assert that the predecessor events were both consumed by the message listener
        assertMessageCountHandledByInbox(3);
        assertMessageConsumedByListener(predecessorEvent);
        assertSequencedMessageProcessedSuccessfully(predecessorEvent);

        // then: assert that the successor event was consumed by the message listener
        assertMessageConsumedByListener(successorEvent1, successorEvent2);
        assertSequencedMessageProcessedSuccessfully(successorEvent1, successorEvent2);
        assertSequenceClosed(contextId);
    }

    @Test
    void testInbox_tenMessagesWithPredecessor_bufferedAndThenProcessedAfterPredecessorHandled() {
        // given: events with a predecessor
        List<UUID> contextIds = IntStream.range(0, 10)
                .mapToObj(i -> randomContextId()).toList();
        List<JmeSimpleTestEvent> successorEvents = contextIds.stream()
                .map(TestMessages::createJmeSimpleTestEvent)
                .toList();
        List<JmeDeclarationCreatedEvent> predecessorEvents = contextIds.stream()
                .map(TestMessages::createDeclarationCreatedEvent)
                .toList();

        // when: sending the successor events
        successorEvents.forEach(event -> sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event));

        // then: assert that the events were buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(successorEvents.size());
        successorEvents.forEach(event -> {
            assertMessageNotConsumedByListener(event);
            assertMessageStateWaitingAndBuffered(event);
        });

        // when: sending the predecessor events
        predecessorEvents.forEach(event -> sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event));

        // then: assert that the predecessor events were consumed by the message listener
        assertMessageCountHandledByInbox(successorEvents.size() + predecessorEvents.size());
        predecessorEvents.forEach(event -> {
            assertMessageConsumedByListener(event);
            assertSequencedMessageProcessedSuccessfully(event);
        });

        // then: assert that the successor events were consumed by the message listener
        successorEvents.forEach(event -> {
            assertMessageConsumedByListener(event);
            assertSequencedMessageProcessedSuccessfully(event);
        });
        contextIds.forEach(contextId -> {
            assertSequenceOfMessages(contextId, predecessorEvents.get(contextIds.indexOf(contextId)), successorEvents.get(contextIds.indexOf(contextId)));
            assertSequenceClosed(contextId);
        });
    }

    @Test
    void testInbox_sequenceDefinitionWithOneMessage_processedSuccessfully() {
        // given: a test event
        JmeEnumTestEvent event = createEnumTestEvent();

        // when: sending the event
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, event);

        // then: assert that the event was consumed by the message listener
        assertMessageConsumedByListener(event);
        assertSequencedMessageProcessedSuccessfully(event);
        assertSequenceClosed(event);
    }

    @Test
    void testInbox_messageWithoutPredecessor_processedSuccessfullyAfterRetry() {
        // given: a test event with a failure message
        JmeDeclarationCreatedEvent event = createDeclarationCreatedEvent(randomContextId());
        event.getPayload().setMessage(DeclarationCreatedEventListener.FAILURE);

        // when: sending the event
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event);

        // then: assert that the event was sent to the EHS and marked as failed
        assertMessageSentToErrorHandlingService(event);
        assertMessageCountHandledByInbox(1);
        assertMessageNotConsumedByListener(event);
        assertSequenceOpen(event);
        assertMessageStateFailedAndNotBuffered(event);

        // when: sending the event again, this time without the failure message
        event.getPayload().setMessage("success");
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event);

        // then: assert that the event was consumed by the message listener
        assertMessageConsumedByListener(event);
        assertMessageCountHandledByInbox(2);
        assertSequencedMessageProcessedSuccessfully(event);
        assertSequenceOpen(event);
    }

    @Test
    void testInbox_messageWithoutPredecessor_sameMessageTypeProcessedSuccessfullyTwice() {
        // given: a test event
        UUID contextId = randomContextId();
        JmeDeclarationCreatedEvent event1 = createDeclarationCreatedEvent(contextId);
        JmeDeclarationCreatedEvent event2 = createDeclarationCreatedEvent(contextId);

        // when: sending the event
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event1);
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event2);

        // then: assert that the event2 were consumed by the message listener
        assertMessageConsumedByListener(event1, event2);
        assertSequencedMessageProcessedSuccessfully(event1, event2);
        assertSequenceOpen(event1, event2);
    }

    @Test
    void testInbox_messageWithPredecessor_bufferedAndThenSentToErrorHandlingOnValueDeserializationError() {
        // given: an event with a predecessor
        UUID contextId = randomContextId();
        JmeSimpleTestEvent successorEvent = createJmeSimpleTestEvent(contextId);

        // when: sending the successor event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, successorEvent);

        // then: assert that the event was buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(1);
        assertMessageNotConsumedByListener(successorEvent);
        assertMessageStateWaitingAndBuffered(successorEvent);

        // when: provoking a deserialization error for the record value
        setBufferedMessageValueToGarbage(contextId);

        // when: sending the predecessor event for the same context ID
        JmeDeclarationCreatedEvent predecessorEvent = createDeclarationCreatedEvent(contextId);
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, predecessorEvent);

        // then: assert that the successor event was sent to the EHS
        assertMessageStateFailed(successorEvent);
        MessageProcessingFailedEvent mpfe = messageRecorder.getExactlyOneMessageProcessingFailedEvent();
        assertThat(mpfe.getPayload().getErrorMessage())
                .containsIgnoringCase("deserialization");
    }

    @Test
    void testInbox_messageWithPredecessor_bufferedAndThenSentToErrorHandlingOnKeyDeserializationError() {
        // given: an event with a predecessor
        UUID contextId = randomContextId();
        JmeSimpleTestEvent successorEvent = createJmeSimpleTestEvent(contextId);

        // when: sending the successor event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, successorEvent);

        // then: assert that the event was buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(1);
        assertMessageNotConsumedByListener(successorEvent);
        assertMessageStateWaitingAndBuffered(successorEvent);

        // when: provoking a deserialization error for the record value
        setBufferedMessageKeyToGarbage(contextId);

        // when: sending the predecessor event for the same context ID
        JmeDeclarationCreatedEvent predecessorEvent = createDeclarationCreatedEvent(contextId);
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, predecessorEvent);

        // then: assert that the successor event was sent to the EHS
        assertMessageStateFailed(successorEvent);
        MessageProcessingFailedEvent mpfe = messageRecorder.getExactlyOneMessageProcessingFailedEvent();
        assertThat(mpfe.getPayload().getErrorMessage())
                .containsIgnoringCase("deserialization");
    }

    private void setBufferedMessageValueToGarbage(UUID contextId) {
        assertThat(jdbcTemplate.update(
                "UPDATE buffered_message SET message_value = ? WHERE sequence_instance_id = (SELECT id FROM sequence_instance WHERE context_id = ?)",
                "garbage".getBytes(UTF_8), contextId.toString()))
                .isEqualTo(1);
    }

    private void setBufferedMessageKeyToGarbage(UUID contextId) {
        assertThat(jdbcTemplate.update(
                "UPDATE buffered_message SET message_key = ? WHERE sequence_instance_id = (SELECT id FROM sequence_instance WHERE context_id = ?)",
                "garbage".getBytes(UTF_8), contextId.toString()))
                .isEqualTo(1);
    }
}
