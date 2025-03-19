package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.IceCreamFlavour;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.JmeEnumTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.*;

@Slf4j
@TestPropertySource(properties = "jeap.messaging.sequential-inbox.config-location=classpath:/messaging/jeap-sequential-inbox-subtypes.yml")
class SequentialInboxSubtypeIT extends SequentialInboxITBase {

    /**
     * This test follows the sequence definition which is defined in the following order:
     * <ol>
     *     <li>JmeDeclarationCreatedEvent</li>
     *     <li>JmeSimpleTestEvent.STRAWBERRY or JmeSimpleTestEvent.VANILLA</li>
     *     <li>JmeEnumTestEvent</li>
     *     <li>JmeSimpleTestEvent.CHOCOLATE</li>
     * </ol>
     */

    @Test
    void testInbox_messageWithoutPredecessor_processedSuccessfully() {
        // given: a test event
        UUID contextId = randomContextId();
        AvroMessage event = createDeclarationCreatedEvent(contextId);

        // when: sending the event
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event);

        // then: assert that the event was consumed by the message listener
        assertMessageConsumedByListener(event);
        assertSequencedMessageProcessedSuccessfully(event);
        assertSequenceOpen(event);
        assertBufferedMessageCount(contextId, 0);
    }

    @Test
    void testInbox_messagesWithPredecessors_waitingAndBuffered() {
        // given: test events with a predecessor
        UUID contextId = randomContextId();
        AvroMessage event1 = createJmeSimpleTestEvent(contextId, IceCreamFlavour.CHOCOLATE);
        AvroMessage event2 = createJmeSimpleTestEvent(contextId, IceCreamFlavour.STRAWBERRY);
        AvroMessage event3 = createJmeSimpleTestEvent(contextId, IceCreamFlavour.VANILLA);
        AvroMessage event4 = createJmeSimpleTestEvent(contextId, IceCreamFlavour.CHOCOLATE);
        AvroMessage event5 = createEnumTestEvent(contextId);

        // when: sending the events
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event1);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event2);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event3);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event4);
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, event5);

        // then: assert that the events were buffered and not consumed by the message listener
        assertMessageCountHandledByInbox(5);
        assertMessageNotConsumedByListener(event1, event2, event3, event4, event5);
        assertMessageStateWaitingAndBuffered(event1, "JmeSimpleTestEvent.CHOCOLATE");
        assertMessageStateWaitingAndBuffered(event2, "JmeSimpleTestEvent.STRAWBERRY");
        assertMessageStateWaitingAndBuffered(event3, "JmeSimpleTestEvent.VANILLA");
        assertMessageStateWaitingAndBuffered(event4, "JmeSimpleTestEvent.CHOCOLATE");
        assertMessageStateWaitingAndBuffered(event5);
        assertSequenceOpen(contextId);
        assertBufferedMessageCount(contextId, 5);
    }

    @Test
    void testInbox_messagesInSequence_processedWithoutBuffering() {
        // given: test events with a predecessor
        UUID contextId = randomContextId();
        AvroMessage event1 = createDeclarationCreatedEvent(contextId);
        AvroMessage event2 = createJmeSimpleTestEvent(contextId, IceCreamFlavour.VANILLA);
        AvroMessage event3 = createJmeSimpleTestEvent(contextId, IceCreamFlavour.STRAWBERRY);
        AvroMessage event4 = createEnumTestEvent(contextId);
        AvroMessage event5 = createJmeSimpleTestEvent(contextId, IceCreamFlavour.CHOCOLATE);

        // when: sending the events
        // then: assert that the events were consumed by their message listener
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event1);
        assertSequencedMessageProcessedSuccessfully(event1);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event2);
        assertSequencedMessageProcessedSuccessfully(event2, "JmeSimpleTestEvent.VANILLA");
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event3);
        assertSequencedMessageProcessedSuccessfully(event3, "JmeSimpleTestEvent.STRAWBERRY");
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, event4);
        assertSequencedMessageProcessedSuccessfully(event4);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event5);
        assertSequencedMessageProcessedSuccessfully(event5, "JmeSimpleTestEvent.CHOCOLATE");

        // then: assert that no messages were buffered and the sequence is closed
        assertMessageCountHandledByInbox(5);
        assertBufferedMessageCount(contextId, 0);
        assertSequenceClosed(contextId);
    }

    @Test
    void testInbox_messagesWithPredecessors_waitingAndBuffered_thenReleasedAfterPredecessorReceived() {
        // given: test events with a predecessor
        UUID contextId = randomContextId();
        AvroMessage event1 = createJmeSimpleTestEvent(contextId, IceCreamFlavour.CHOCOLATE);
        AvroMessage event2 = createJmeSimpleTestEvent(contextId, IceCreamFlavour.STRAWBERRY);
        AvroMessage event3 = createJmeSimpleTestEvent(contextId, IceCreamFlavour.VANILLA);
        AvroMessage event4 = createJmeSimpleTestEvent(contextId, IceCreamFlavour.CHOCOLATE);
        AvroMessage event5 = createEnumTestEvent(contextId);
        AvroMessage predecessor = createDeclarationCreatedEvent(contextId);

        // when: sending the events
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event1);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event2);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event3);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event4);
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, event5);

        // then: assert that the events were buffered and not consumed by the message listener
        assertMessageCountHandledByInbox(5);
        assertMessageStateWaitingAndBuffered(event1, "JmeSimpleTestEvent.CHOCOLATE");
        assertMessageStateWaitingAndBuffered(event2, "JmeSimpleTestEvent.STRAWBERRY");
        assertMessageStateWaitingAndBuffered(event3, "JmeSimpleTestEvent.VANILLA");
        assertMessageStateWaitingAndBuffered(event4, "JmeSimpleTestEvent.CHOCOLATE");
        assertMessageStateWaitingAndBuffered(event5);
        assertBufferedMessageCount(contextId, 5);

        // when: sending the predecessor
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, predecessor);

        // then: assert that the events were consumed by the message listener
        assertMessageCountHandledByInbox(6);
        assertMessageConsumedByListener(event1, event2, event3, event4, event5, predecessor);
        assertSequencedMessageProcessedSuccessfully(event1, "JmeSimpleTestEvent.CHOCOLATE");
        assertSequencedMessageProcessedSuccessfully(event2, "JmeSimpleTestEvent.STRAWBERRY");
        assertSequencedMessageProcessedSuccessfully(event3, "JmeSimpleTestEvent.VANILLA");
        assertSequencedMessageProcessedSuccessfully(event4, "JmeSimpleTestEvent.CHOCOLATE");
        assertSequencedMessageProcessedSuccessfully(event5, predecessor);
        assertSequenceClosed(contextId);
    }

    @Test
    void testInbox_messageWithOrPredecessor_waitingAndBuffered_thenReleasedAfterPredecessorReceived() {
        // given: test events with a predecessor
        UUID contextId = randomContextId();
        AvroMessage firstEvent = createDeclarationCreatedEvent(contextId);
        AvroMessage predecessor = createJmeSimpleTestEvent(contextId, IceCreamFlavour.VANILLA);
        AvroMessage successor = createEnumTestEvent(contextId);

        // when: sending the events except for the predecessor
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, firstEvent);
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, successor);

        // then: assert that the first event was processed and the successor event was buffered
        assertMessageCountHandledByInbox(2);
        assertMessageConsumedByListener(firstEvent);
        assertSequencedMessageProcessedSuccessfully(firstEvent);
        assertMessageStateWaitingAndBuffered(successor);

        // when: sending the predecessor of the third event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, predecessor);

        // then: assert that the predecessor and successor events were consumed by the message listener
        assertMessageCountHandledByInbox(3);
        assertSequencedMessageProcessedSuccessfully(predecessor, "JmeSimpleTestEvent.VANILLA");
        assertSequencedMessageProcessedSuccessfully(successor);
        assertMessageConsumedByListener(predecessor, successor);
        assertSequenceOpen(contextId);

        // when: sending the remaining events
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, createJmeSimpleTestEvent(contextId, IceCreamFlavour.STRAWBERRY));
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, createJmeSimpleTestEvent(contextId, IceCreamFlavour.CHOCOLATE));

        // then: assert that the sequence is closed
        assertSequenceClosed(contextId);
    }

    @Test
    void testInbox_badSubtypeSentToErrorHandlingService() {
        // given: test events with a predecessor
        UUID contextId = randomContextId();
        // the default message in the event is "test", which is not a valid enum value
        JmeSimpleTestEvent eventWithBadSubType = createJmeSimpleTestEvent(contextId);

        // when: sending the event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, eventWithBadSubType);

        // then: assert that the event has been sent to the error handling service
        assertMessageCountHandledByInbox(1);
        assertMessageSentToErrorHandlingService(eventWithBadSubType);
    }
}
