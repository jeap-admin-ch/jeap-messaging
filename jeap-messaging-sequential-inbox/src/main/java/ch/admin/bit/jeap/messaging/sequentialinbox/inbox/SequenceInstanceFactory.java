package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.Sequence;
import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.SequenceInstanceRepository;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequenceInstance;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
class SequenceInstanceFactory {

    private final SequenceInstanceRepository repository;
    private final Transactions tx;
    private final int idleLockTimeoutSeconds;

    SequenceInstanceFactory(SequenceInstanceRepository repository,
                            Transactions tx,
                            @Value("${jeap.messaging.sequential-inbox.idle-lock-timeout-seconds:600}") int idleLockTimeoutSeconds) {
        this.repository = repository;
        this.tx = tx;
        this.idleLockTimeoutSeconds = idleLockTimeoutSeconds;
        log.info("Setting idle lock timeout to {} seconds for sequential inbox transactions", idleLockTimeoutSeconds);
    }

    SequenceInstance getExistingSequenceInstanceAndLockForUpdate(long sequenceInstanceId) {
        return repository.getByIdAndLockForUpdate(sequenceInstanceId, idleLockTimeoutSeconds);
    }

    long createOrGetSequenceInstance(Sequence sequence, String contextId) {
        try {
            return tx.callInNewTransaction(() -> {
                // First, attempt to find an existing instance
                Optional<Long> existingInstance = repository.findIdByNameAndContextId(sequence.getName(), contextId);
                // Return the existing instance if found, otherwise create a new one
                return existingInstance.orElseGet(() -> saveNewInstance(sequence, contextId));
            });
        } catch (DataIntegrityViolationException e) {
            // If another thread inserted the same contextId concurrently, find and return it
            // Note: The transaction will be rolled back upon a DataIntegrityViolationException, so this needs to happen
            // in a new transaction.
            return tx.callInNewTransaction(() -> repository.findIdByNameAndContextId(sequence.getName(), contextId))
                    .orElseThrow(() -> SequentialInboxException.failedToReadExistingSequenceInstance(sequence, contextId, e));
        }
    }

    private long saveNewInstance(Sequence sequence, String contextId) {
        SequenceInstance newInstance = SequenceInstance.builder()
                .contextId(contextId)
                .name(sequence.getName())
                .retentionPeriod(sequence.getRetentionPeriod())
                .build();
        return repository.saveNewInstance(newInstance);
    }
}
