package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jeap.messaging.sequentialinbox.housekeeping.SequentialInboxHousekeepingService;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.TestMessages.*;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class SequentialInboxHousekeepingIT extends SequentialInboxITBase {

    @Autowired
    private SequentialInboxHousekeepingService housekeepingService;

    @Test
    void testHousekeeping_deleteExpiredMessages() {
        // given: a test event with a predecessor that is buffered and waiting
        UUID contextIdExpired = randomContextId();
        UUID contextIdNotExpired = randomContextId();
        createOpenSequence(contextIdExpired);
        createOpenSequence(contextIdNotExpired);

        // given: expire the open sequence
        expireSequence(contextIdExpired);

        // when: run housekeeping
        housekeepingService.deleteExpiredMessages();

        // then: the expired sequence and its messages are deleted
        assertThat(findSequenceInstanceByContextId(contextIdExpired.toString()))
                .isEmpty();
        assertThat(countSequencedMessages(contextIdExpired))
                .isZero();
        assertThat(countBufferedMessages(contextIdExpired))
                .isZero();

        // then: the not expired sequence and its messages are still there
        assertThat(findSequenceInstanceByContextId(contextIdNotExpired.toString()))
                .isNotEmpty();
        assertThat(countSequencedMessages(contextIdNotExpired))
                .isOne();
        assertThat(countBufferedMessages(contextIdNotExpired))
                .isOne();
    }

    @Test
    void testHousekeeping_deleteClosedSequences() {
        // given: a test event with a predecessor that is buffered and waiting
        UUID contextIdClosed = randomContextId();
        UUID contextIdOpen = randomContextId();
        createClosedSequence(contextIdClosed);
        createOpenSequence(contextIdOpen);

        // when: run housekeeping
        housekeepingService.deleteClosedSequenceInstances();

        // then: the closed sequence and its messages are deleted
        assertThat(findSequenceInstanceByContextId(contextIdClosed.toString()))
                .isEmpty();
        assertThat(countSequencedMessages(contextIdClosed))
                .isZero();
        assertThat(countBufferedMessages(contextIdClosed))
                .isZero();

        // then: the open sequence and its messages are still there
        assertThat(findSequenceInstanceByContextId(contextIdOpen.toString()))
                .isNotEmpty();
        assertThat(countSequencedMessages(contextIdOpen))
                .isOne();
        assertThat(countBufferedMessages(contextIdOpen))
                .isOne();
    }

    private int countBufferedMessages(UUID contextId) {
        return jdbcTemplate.queryForObject("""
                        SELECT count(*) FROM buffered_message WHERE sequence_instance_id = 
                        (SELECT id FROM sequence_instance WHERE context_id = ?)
                        """, Integer.class,
                contextId.toString());
    }

    private int countSequencedMessages(UUID contextId) {
        return jdbcTemplate.queryForObject("""
                        SELECT count(*) FROM buffered_message WHERE sequence_instance_id = 
                        (SELECT id FROM sequence_instance WHERE context_id = ?)
                        """, Integer.class,
                contextId.toString());
    }

    private void expireSequence(UUID contextIdExpired) {
        jdbcTemplate.update("UPDATE sequence_instance SET retain_until = NOW() - INTERVAL '1 year' WHERE context_id = ?", contextIdExpired.toString());
    }

    private void createClosedSequence(UUID contextId) {
        JmeDeclarationCreatedEvent predecessorEvent = createDeclarationCreatedEvent(contextId);
        sendSync(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, predecessorEvent);
        JmeSimpleTestEvent successorEvent = createJmeSimpleTestEvent(contextId);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, successorEvent);
        assertSequenceClosed(contextId);
    }

    private void createOpenSequence(UUID contextId) {
        JmeSimpleTestEvent event = createJmeSimpleTestEvent(contextId);
        sendSync(JmeSimpleTestEvent.TypeRef.DEFAULT_TOPIC, event);
        assertMessageStateWaitingAndBuffered(event);
        assertSequenceOpen(event);
    }
}
