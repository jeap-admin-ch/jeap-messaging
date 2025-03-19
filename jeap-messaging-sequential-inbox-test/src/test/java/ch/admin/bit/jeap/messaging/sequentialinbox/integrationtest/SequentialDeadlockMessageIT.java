package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jeap.messaging.sequentialinbox.inbox.Transactions;
import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.SequenceInstanceRepository;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequenceInstance;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.JmeEnumTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.*;

@TestPropertySource(properties = "jeap.messaging.sequential-inbox.config-location=classpath:/messaging/jeap-sequential-inbox-three-messages.yml")
class SequentialDeadlockMessageIT extends SequentialInboxITBase {

    @Autowired
    private SequenceInstanceRepository sequenceInstanceRepository;

    @Autowired
    private Transactions transactions;

    private CountDownLatch latch;

    @BeforeEach
    void setUpLatch() {
        latch = new CountDownLatch(1);
    }

    @Test
    void testInbox_twoMessagesWithPredecessors_bufferedAndThenProcessedAfterPredecessorHandledAndDeadLockedTheProcessInstance()
            throws InterruptedException {
        // given: an event with a predecessor
        UUID contextId = randomContextId();
        JmeDeclarationCreatedEvent firstEvent = createDeclarationCreatedEvent(contextId);
        JmeSimpleTestEvent secondEvent = createJmeSimpleTestEvent(contextId);
        JmeEnumTestEvent thirdEvent = createEnumTestEvent(contextId);

        // when: sending the first event
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, firstEvent);

        // then: assert that the predecessor event was consumed by the message listener
        assertMessageConsumedByListener(firstEvent);
        assertSequencedMessageProcessedSuccessfully(firstEvent);
        assertMessageCountHandledByInbox(1);

        // when: one message dead locks the sequence instance
        // Simulated, by starting a new thread to lock the sequence instance for update
        CountDownLatch latchThread = new CountDownLatch(1);
        Thread lockThread = new Thread(() -> {
            transactions.runInNewTransaction(() -> {
                long sequenceInstanceId = sequenceInstanceRepository.findIdByNameAndContextId("ThreeSequentialEvents", contextId.toString()).orElseThrow(() -> new IllegalStateException("Sequence instance not found"));
                SequenceInstance sequenceInstanceLocked = sequenceInstanceRepository.getByIdAndLockForUpdate(sequenceInstanceId, 1000);
                try {
                    latchThread.countDown();
                    latch.await(); // Hold the lock
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });
        lockThread.start();

        // Ensure the latch is awaited before continuing, give some time for the lock to be acquired
        latchThread.await();

        // when: sending the third event
        sendSync(JmeEnumTestEvent.TypeRef.DEFAULT_TOPIC, thirdEvent);

        // then: the third event should still be buffered and not processed
        // and: not be blocked by the dead lock
        assertMessageNotConsumedByListener(thirdEvent);
        assertMessageStateWaitingAndBuffered(thirdEvent);

        // when: Releasing the lock
        latch.countDown();
        lockThread.join();

        // and: sending the second event
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, secondEvent);

        // then: assert that the successor events were consumed by the message listeners
        // and: the third event should be consumed by the message listener and processed
        assertMessageCountHandledByInbox(3);
        assertMessageConsumedByListener(secondEvent);
        assertSequencedMessageProcessedSuccessfully(secondEvent);
        assertMessageConsumedByListener(thirdEvent);
        assertSequencedMessageProcessedSuccessfully(thirdEvent);
        assertSequenceOfMessages(contextId, firstEvent, secondEvent, thirdEvent);
        assertSequenceClosed(contextId);
    }
}
