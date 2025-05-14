package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.DeclarationCreatedEventListener;
import ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.MultipleTestEventListener;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.JmeEnumTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@TestPropertySource(properties = "jeap.messaging.sequential-inbox.config-location=classpath:/messaging/jeap-sequential-inbox-three-messages.yml")
class SequentialInboxThreeSequentialMessageIT extends SequentialInboxITBase {

    @MockitoBean
    private JeapKafkaMessageCallback jeapKafkaMessageCallback;

    @Test
    void testInbox_twoMessagesWithPredecessors_bufferedAndThenProcessedAfterPredecessorHandled() {
        // given: an event with a predecessor
        UUID contextId = randomContextId();
        JmeDeclarationCreatedEvent firstEvent = createDeclarationCreatedEvent(contextId);
        JmeSimpleTestEvent secondEvent = createJmeSimpleTestEvent(contextId);
        JmeEnumTestEvent thirdEvent = createEnumTestEvent(contextId);

        // when: sending the third and second event
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, thirdEvent);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, secondEvent);

        // then: assert that both events were buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(2);
        assertMessageNotConsumedByListener(secondEvent);
        assertMessageStateWaitingAndBuffered(secondEvent);
        assertMessageNotConsumedByListener(thirdEvent);
        assertMessageStateWaitingAndBuffered(thirdEvent);

        // when: sending the predecessor event for the same context ID
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, firstEvent);

        // then: assert that the predecessor event was consumed by the message listener
        assertMessageCountHandledByInbox(3);
        assertMessageConsumedByListener(firstEvent);
        assertSequencedMessageProcessedSuccessfully(firstEvent);

        // then: assert that the successor events were consumed by the message listeners
        assertMessageConsumedByListener(secondEvent);
        assertMessageConsumedByListener(thirdEvent);
        assertSequencedMessageProcessedSuccessfully(secondEvent);
        assertSequencedMessageProcessedSuccessfully(thirdEvent);
        assertSequenceOfMessages(contextId, firstEvent, secondEvent, thirdEvent);
        assertSequenceClosed(contextId);

        verify(jeapKafkaMessageCallback).beforeConsume(firstEvent, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterConsume(firstEvent, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterRecord(firstEvent, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC);

        verify(jeapKafkaMessageCallback).beforeConsume(secondEvent, JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterConsume(secondEvent, JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterRecord(secondEvent, JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC);

        verify(jeapKafkaMessageCallback).beforeConsume(thirdEvent, JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterConsume(thirdEvent, JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterRecord(thirdEvent, JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC);
    }

    @Test
    void testInbox_threeMessagesInCorrectSequence_notBuffered() {
        // given: three events in correct sequence
        UUID contextId = randomContextId();
        JmeDeclarationCreatedEvent firstEvent = createDeclarationCreatedEvent(contextId);
        JmeSimpleTestEvent secondEvent = createJmeSimpleTestEvent(contextId);
        JmeEnumTestEvent thirdEvent = createEnumTestEvent(contextId);

        // when: sending the first event
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, firstEvent);

        // then: assert that the first event was consumed by the message listener
        assertMessageCountHandledByInbox(1);
        assertMessageConsumedByListener(firstEvent);
        assertSequencedMessageProcessedSuccessfully(firstEvent);

        // when: sending the second event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, secondEvent);

        // then: assert that the second event was consumed by the message listener
        assertMessageCountHandledByInbox(2);
        assertMessageConsumedByListener(secondEvent);
        assertSequencedMessageProcessedSuccessfully(secondEvent);
        assertSequenceOpen(contextId);

        // when: sending the third event
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, thirdEvent);

        // then: assert that the third event was consumed by the message listener
        assertMessageCountHandledByInbox(3);
        assertMessageConsumedByListener(thirdEvent);
        assertSequencedMessageProcessedSuccessfully(thirdEvent);

        // then: assert that the sequence of messages was processed in the correct order
        assertSequenceOfMessages(contextId, firstEvent, secondEvent, thirdEvent);
        assertSequenceClosed(contextId);
        assertBufferedMessageCount(contextId, 0);

        verify(jeapKafkaMessageCallback).beforeConsume(firstEvent, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterConsume(firstEvent, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterRecord(firstEvent, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC);

        verify(jeapKafkaMessageCallback).beforeConsume(secondEvent, JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterConsume(secondEvent, JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterRecord(secondEvent, JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC);

        verify(jeapKafkaMessageCallback).beforeConsume(thirdEvent, JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterConsume(thirdEvent, JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterRecord(thirdEvent, JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC);
    }

    @Test
    void testInbox_twoMessagesWithPredecessors_bufferedUntilPredecessorProcessedAfterFailure() {
        // given: an event with a predecessor
        UUID contextId = randomContextId();
        JmeDeclarationCreatedEvent firstEvent = createDeclarationCreatedEvent(contextId);
        JmeSimpleTestEvent secondEvent = createJmeSimpleTestEvent(contextId);
        JmeEnumTestEvent thirdEvent = createEnumTestEvent(contextId);

        // when: sending the third and second event
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, thirdEvent);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, secondEvent);

        // then: assert that both events were buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(2);
        assertMessageNotConsumedByListener(secondEvent);
        assertMessageStateWaitingAndBuffered(secondEvent);
        assertMessageNotConsumedByListener(thirdEvent);
        assertMessageStateWaitingAndBuffered(thirdEvent);

        // when: sending the predecessor event for the same context ID, failing the processing
        firstEvent.getPayload().setMessage(DeclarationCreatedEventListener.FAILURE);
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, firstEvent);

        // then: assert that the event was sent to the EHS and marked as failed
        assertMessageSentToErrorHandlingService(firstEvent);
        assertMessageCountHandledByInbox(3);
        assertMessageNotConsumedByListener(firstEvent);
        assertSequenceOpen(firstEvent);
        assertMessageStateFailedAndNotBuffered(firstEvent);
        // then: assert that the successor events are still buffered
        assertMessageStateWaitingAndBuffered(secondEvent, thirdEvent);

        // when: sending the predecessor event for the same context ID, this time successfully
        firstEvent.getPayload().setMessage("success");
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, firstEvent);

        // then: assert that the predecessor event was consumed by the message listener
        assertMessageCountHandledByInbox(4);
        assertMessageConsumedByListener(firstEvent);
        assertSequencedMessageProcessedSuccessfully(firstEvent);

        // then: assert that the successor events were consumed by the message listeners
        assertMessageConsumedByListener(secondEvent);
        assertMessageConsumedByListener(thirdEvent);
        assertSequencedMessageProcessedSuccessfully(secondEvent);
        assertSequencedMessageProcessedSuccessfully(thirdEvent);
        assertSequenceOfMessages(contextId, firstEvent, secondEvent, thirdEvent);
        assertSequenceClosed(contextId);

        verify(jeapKafkaMessageCallback).beforeConsume(firstEvent, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterConsume(firstEvent, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterRecord(firstEvent, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC);

        verify(jeapKafkaMessageCallback).beforeConsume(secondEvent, JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterConsume(secondEvent, JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterRecord(secondEvent, JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC);

        verify(jeapKafkaMessageCallback).beforeConsume(thirdEvent, JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterConsume(thirdEvent, JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterRecord(thirdEvent, JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC);
    }

    @Test
    void testInbox_twoMessagesWithPredecessors_bufferedAndThenProcessedAfterPredecessorHandled_failFirstSuccessorThenRetry() {
        // given: an event with a predecessor
        UUID contextId = randomContextId();
        JmeDeclarationCreatedEvent firstEvent = createDeclarationCreatedEvent(contextId);
        JmeSimpleTestEvent secondEvent = createJmeSimpleTestEvent(contextId);
        JmeEnumTestEvent thirdEvent = createEnumTestEvent(contextId);

        // when: sending the third and second event
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, thirdEvent);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, secondEvent);

        // then: assert that both events were buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(2);
        assertMessageNotConsumedByListener(secondEvent);
        assertMessageStateWaitingAndBuffered(secondEvent);
        assertMessageNotConsumedByListener(thirdEvent);
        assertMessageStateWaitingAndBuffered(thirdEvent);

        // when: sending the predecessor event for the same context ID, and failing processing of the first successor
        MultipleTestEventListener.failOnJmeSimpleTestEvent = true;
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, firstEvent);

        // then: assert that the predecessor event was consumed by the message listener
        assertMessageCountHandledByInbox(3);
        assertMessageConsumedByListener(firstEvent);
        assertSequencedMessageProcessedSuccessfully(firstEvent);

        // then: assert that the second event failed to process and was sent to the EHS
        assertMessageSentToErrorHandlingService(secondEvent);
        assertMessageNotConsumedByListener(secondEvent);
        assertMessageStateFailed(secondEvent);

        // when: retrying the failed message
        MultipleTestEventListener.failOnJmeSimpleTestEvent = false;
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, secondEvent);

        // then: assert that the successor events were bot consumed by the message listeners and the sequence is closed
        assertMessageConsumedByListener(secondEvent);
        assertMessageConsumedByListener(thirdEvent);
        assertSequencedMessageProcessedSuccessfully(secondEvent);
        assertSequencedMessageProcessedSuccessfully(thirdEvent);
        assertSequenceOfMessages(contextId, firstEvent, secondEvent, thirdEvent);
        assertSequenceClosed(contextId);

        verify(jeapKafkaMessageCallback).beforeConsume(firstEvent, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterConsume(firstEvent, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterRecord(firstEvent, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC);

        verify(jeapKafkaMessageCallback, times(2)).beforeConsume(secondEvent, JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterConsume(secondEvent, JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback, times(2)).afterRecord(secondEvent, JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC);

        verify(jeapKafkaMessageCallback).beforeConsume(thirdEvent, JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterConsume(thirdEvent, JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC);
        verify(jeapKafkaMessageCallback).afterRecord(thirdEvent, JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC);
    }
}
