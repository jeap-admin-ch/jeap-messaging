package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.JmeEnumTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.*;

@Slf4j
@TestPropertySource(properties = "jeap.messaging.sequential-inbox.config-location=classpath:/messaging/jeap-sequential-inbox-three-messages.yml")
class SequentialInboxRecordingIT extends SequentialInboxWithPreRecordingITBase {

    @Test
    void testPredecessorEventsProcessedAfterRecordingEnds() {
        // given: an event with a predecessor and the record mode is enabled
        UUID contextId = randomContextId();
        JmeDeclarationCreatedEvent firstEvent = createDeclarationCreatedEvent(contextId);
        JmeSimpleTestEvent secondEvent = createJmeSimpleTestEvent(contextId);
        JmeEnumTestEvent thirdEvent = createEnumTestEvent(contextId);

        // when: sending the third event
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, thirdEvent);

        //then: assert that the third event was consumed by the message listener
        assertSequencedMessageProcessedSuccessfully(thirdEvent);
        assertMessageConsumedByListener(thirdEvent);

        // given: time passing until sequencing starts (record mode gets disabled)
        whenRecordingEndsAndSequencingStarts();

        // when: sending the second event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, secondEvent);

        // then: assert that the second event was buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(2);
        assertMessageNotConsumedByListener(secondEvent);
        assertMessageStateWaitingAndBuffered(secondEvent);

        // when: sending the predecessor event for the same context ID
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, firstEvent);

        // then: assert that the predecessor event was consumed by the message listener
        assertMessageConsumedByListener(firstEvent);
        assertSequencedMessageProcessedSuccessfully(firstEvent);
        assertMessageCountHandledByInbox(3);

        // then: assert that the successor events were consumed by the message listeners
        assertMessageConsumedByListener(secondEvent);
        assertMessageConsumedByListener(thirdEvent);
        assertSequencedMessageProcessedSuccessfully(secondEvent);
        assertSequencedMessageProcessedSuccessfully(thirdEvent);
        assertSequenceOfMessages(contextId, thirdEvent, firstEvent, secondEvent);
        assertSequenceClosed(contextId);
    }

    @Test
    void testPredecessorEventsAlreadyProcessedDuringRecording() {
        // given: an event with a predecessor and the record mode is enabled
        UUID contextId = randomContextId();
        JmeDeclarationCreatedEvent firstEvent = createDeclarationCreatedEvent(contextId);
        JmeSimpleTestEvent secondEvent = createJmeSimpleTestEvent(contextId);
        JmeEnumTestEvent thirdEvent = createEnumTestEvent(contextId);

        // when: sending the first event
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, firstEvent);

        //then: assert that the first event was consumed by the message listener
        assertMessageConsumedByListener(firstEvent);
        assertSequencedMessageProcessedSuccessfully(firstEvent);

        // when: sending the second event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, secondEvent);

        //then: assert that the second event was consumed by the message listener
        assertMessageConsumedByListener(secondEvent);
        assertSequencedMessageProcessedSuccessfully(secondEvent);

        // given: time passing until sequencing starts (record mode gets disabled)
        whenRecordingEndsAndSequencingStarts();

        // when: sending the third event
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, thirdEvent);

        // then: assert that the third event was buffered and consumed by the message listener
        assertMessageCountHandledByInbox(3);
        assertMessageConsumedByListener(thirdEvent);
        assertSequencedMessageProcessedSuccessfully(thirdEvent);
        assertSequenceOfMessages(contextId, firstEvent, secondEvent, thirdEvent);
        assertSequenceClosed(contextId);
    }

    @Test
    void testAllMessagesProcessedBeforeRecordingEnds() {
        // given: an event with a predecessor and the record mode is enabled
        UUID contextId = randomContextId();
        JmeDeclarationCreatedEvent firstEvent = createDeclarationCreatedEvent(contextId);
        JmeSimpleTestEvent secondEvent = createJmeSimpleTestEvent(contextId);
        JmeEnumTestEvent thirdEvent = createEnumTestEvent(contextId);

        // when: sending all events in an incorrect order
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, thirdEvent);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, secondEvent);
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, firstEvent);

        //then: assert that alls events were consumed by the message listener
        assertMessageConsumedByListener(thirdEvent);
        assertSequencedMessageProcessedSuccessfully(thirdEvent);
        assertMessageConsumedByListener(secondEvent);
        assertSequencedMessageProcessedSuccessfully(secondEvent);
        assertMessageConsumedByListener(firstEvent);
        assertSequencedMessageProcessedSuccessfully(firstEvent);

        // then: assert that the sequence is closed
        assertSequenceClosed(contextId);
    }
}
