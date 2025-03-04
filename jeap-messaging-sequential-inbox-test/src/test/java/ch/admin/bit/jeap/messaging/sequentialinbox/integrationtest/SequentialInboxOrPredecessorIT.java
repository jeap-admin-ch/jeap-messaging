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
@TestPropertySource(properties = "jeap.messaging.sequential-inbox.config-location=classpath:/messaging/jeap-sequential-inbox-or-predecessor.yml")
class SequentialInboxOrPredecessorIT extends SequentialInboxITBase {

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
    void testInbox_successorHandledAfterPredecessor() {
        // given: an event with two ORed predecessors
        UUID contextId = randomContextId();
        JmeEnumTestEvent successor = createEnumTestEvent(contextId);

        // when: sending the event
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, successor);

        // then: assert that the event was buffered and not yet consumed by the message listener
        assertMessageCountHandledByInbox(1);
        assertMessageNotConsumedByListener(successor);
        assertMessageStateWaitingAndBuffered(successor);
        assertSequenceOpen(contextId);

        // when: sending the predecessor event
        JmeDeclarationCreatedEvent predecessor = createDeclarationCreatedEvent(contextId);
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, predecessor);

        // then: assert that the predecessor event was consumed by the message listener
        assertMessageCountHandledByInbox(2);
        assertMessageConsumedByListener(predecessor);
        assertSequencedMessageProcessedSuccessfully(predecessor);

        // then: assert that the successor event was consumed by the message listener
        assertMessageConsumedByListener(successor);
        assertSequencedMessageProcessedSuccessfully(successor);
        assertSequenceOpen(contextId);

        // when: sending an event for the other predecessor
        JmeSimpleTestEvent otherPredecessor = createJmeSimpleTestEvent(contextId);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, otherPredecessor);

        // then: assert that the other predecessor event was consumed by the message listener
        assertMessageCountHandledByInbox(3);
        assertMessageConsumedByListener(otherPredecessor);
        assertSequencedMessageProcessedSuccessfully(otherPredecessor);
        assertSequenceClosed(contextId);
    }

}
