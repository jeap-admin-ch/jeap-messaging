package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessageState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
interface SpringDataJpaSequencedMessageRepository extends JpaRepository<SequencedMessage, Long> {

    @Query(nativeQuery = true, value = "SELECT message_type FROM sequenced_message WHERE sequence_instance_id = ?1 AND state = 'PROCESSED'")
    Set<String> getProcessedMessageTypesInSequence(Long sequenceInstanceId);

    List<SequencedMessage> findAllBySequenceInstanceIdAndStateIn(long sequenceInstanceId, Set<SequencedMessageState> state);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE sequenced_message SET state = ?2, state_changed_at = NOW() WHERE id = ?1")
    void updateStateById(long sequencedMessageId, String sequencedMessageStateName);

    Optional<SequencedMessage> findByMessageTypeAndIdempotenceId(String messageType, String idempotenceId);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM sequenced_message WHERE sequence_instance_id IN (SELECT id FROM sequence_instance WHERE retain_until < ?1)")
    int deleteExpired(ZonedDateTime cutoffTime);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM sequenced_message WHERE sequence_instance_id IN (SELECT id FROM sequence_instance WHERE state = 'CLOSED')")
    int deleteForClosedSequences();

    @Query(nativeQuery = true, value = "SELECT message_type as messageType, COUNT(*) as stateCount FROM sequenced_message WHERE state = 'WAITING' GROUP BY message_type")
    List<CountByType> getWaitingMessageCountGroupedByMessageType();
}
