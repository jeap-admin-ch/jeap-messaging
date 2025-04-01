package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.BufferedMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessageState;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MessageRepository {

    private static final Set<SequencedMessageState> WAITING_AND_PROCESSED_STATE = Set.of(SequencedMessageState.WAITING, SequencedMessageState.PROCESSED);

    private final SpringDataJpaBufferedMessageRepository bufferedMessageRepository;
    private final SpringDataJpaSequencedMessageRepository sequencedMessageRepository;
    private final SpringDataJpaMessageHeaderRepository messageHeaderRepository;

    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveMessage(BufferedMessage bufferedMessage, SequencedMessage sequencedMessage) {
        SequencedMessage persistentSequencedMessage = sequencedMessageRepository.save(sequencedMessage);
        if (bufferedMessage != null) {
            bufferedMessage.setSequencedMessageId(persistentSequencedMessage.getId());
            bufferedMessageRepository.save(bufferedMessage);
        }
        entityManager.flush();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Set<String> getProcessedMessageTypesInSequenceInNewTransaction(long sequenceInstanceId) {
        return sequencedMessageRepository.getProcessedMessageTypesInSequence(sequenceInstanceId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public List<SequencedMessage> getWaitingAndProcessedMessagesInNewTransaction(long sequenceInstanceId) {
        return sequencedMessageRepository.findAllBySequenceInstanceIdAndStateIn(sequenceInstanceId, WAITING_AND_PROCESSED_STATE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public BufferedMessage getBufferedMessageInNewTransaction(SequencedMessage sequencedMessage) {
        return bufferedMessageRepository.getBySequencedMessageId(sequencedMessage.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void setMessageStateInNewTransaction(SequencedMessage sequencedMessage, SequencedMessageState sequencedMessageState) {
        sequencedMessageRepository.updateStateById(sequencedMessage.getId(), sequencedMessageState.name());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void setMessageStateInCurrentTransaction(SequencedMessage sequencedMessage, SequencedMessageState sequencedMessageState) {
        sequencedMessageRepository.updateStateById(sequencedMessage.getId(), sequencedMessageState.name());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Optional<SequencedMessage> findByMessageTypeAndIdempotenceIdInNewTransaction(String messageType, String idempotenceId) {
        return sequencedMessageRepository.findByMessageTypeAndIdempotenceId(messageType, idempotenceId);
    }


    /**
     * Deletes all buffered messages, message headers and sequenced messages associated with
     * sequence instances whose retention period has elapsed (retain_until < cutoffTime).
     *
     * @param cutoffTime The timestamp to compare against for determining expiration
     * @return The number of sequenced messages deleted
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteExpiredMessages(java.time.ZonedDateTime cutoffTime) {
        messageHeaderRepository.deleteExpired(cutoffTime);
        bufferedMessageRepository.deleteExpired(cutoffTime);
        return sequencedMessageRepository.deleteExpired(cutoffTime);
    }

    /**
     * Deletes all messages (buffered and sequenced, message headers) associated with sequence instances in CLOSED state.
     * @return The number of sequenced messages deleted
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteMessagesForClosedSequences() {
        messageHeaderRepository.deleteForClosedSequences();
        bufferedMessageRepository.deleteForClosedSequences();
        return sequencedMessageRepository.deleteForClosedSequences();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<String, Double> getWaitingMessageCountByType() {
        return sequencedMessageRepository.getWaitingMessageCountGroupedByMessageType().stream()
                .collect(Collectors.toMap(CountByType::messageType, CountByType::waitingCount));
    }
}
