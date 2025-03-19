package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.IceCreamFlavour;
import ch.admin.bit.jme.test.JmeEnumTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.*;

@Slf4j
@TestPropertySource(properties = "jeap.messaging.sequential-inbox.config-location=classpath:/messaging/jeap-sequential-inbox-multisequence-subtypes.yml")
class SequentialInboxMultisequenceSubtypeIT extends SequentialInboxITBase {

    /**
     * This test follows the sequence definitions which are defined as follows:
     * <h2>Sequence 1</h2>
     * <ol>
     *     <li>JmeSimpleTestEvent.STRAWBERRY</li>
     *     <li>JmeSimpleTestEvent.VANILLA</li>
     * </ol>
     * <h2>Sequence 2</h2>
     * <ol>
     *     <li>JmeSimpleTestEvent.CHOCOLATE</li>
     *     <li>JmeEnumTestEvent</li>
     * </ol>
     */

    @Test
    void testInbox_messagesWithPredecessors_waitingAndBuffered() {
        // given: test events
        UUID contextIdSeq1 = randomContextId();
        UUID contextIdSeq2 = randomContextId();
        AvroMessage seq1Predecessor = createJmeSimpleTestEvent(contextIdSeq1, IceCreamFlavour.STRAWBERRY);
        AvroMessage seq1Successor = createJmeSimpleTestEvent(contextIdSeq1, IceCreamFlavour.VANILLA);
        AvroMessage seq2Predecessor = createJmeSimpleTestEvent(contextIdSeq2, IceCreamFlavour.CHOCOLATE);
        AvroMessage seq2Successor = createEnumTestEvent(contextIdSeq2);

        // when: sending the successor events
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, seq1Successor);
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, seq2Successor);

        // then: assert that the events were buffered and not consumed by the message listener
        assertMessageCountHandledByInbox(2);
        assertMessageStateWaitingAndBuffered(seq1Successor, "JmeSimpleTestEvent.VANILLA");
        assertMessageStateWaitingAndBuffered(seq2Successor);
        assertSequenceOpen(contextIdSeq1);
        assertSequenceOpen(contextIdSeq2);

        // when: sending the predecessor events
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, seq1Predecessor);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, seq2Predecessor);

        // then: assert that the predecessor events were consumed by the message listener and the sequence is closed
        assertMessageCountHandledByInbox(4);
        assertMessageConsumedByListener(seq1Predecessor, seq2Predecessor, seq1Successor, seq2Successor);
        assertSequencedMessageProcessedSuccessfully(seq1Predecessor, "JmeSimpleTestEvent.STRAWBERRY");
        assertSequencedMessageProcessedSuccessfully(seq1Successor, "JmeSimpleTestEvent.VANILLA");
        assertSequencedMessageProcessedSuccessfully(seq2Predecessor, "JmeSimpleTestEvent.CHOCOLATE");
        assertSequencedMessageProcessedSuccessfully(seq2Successor);
        assertSequenceClosed(contextIdSeq1);
        assertSequenceClosed(contextIdSeq2);
    }
}
