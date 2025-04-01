package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequenceInstance;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY) // All operations must happen in a transaction
public class SequenceInstanceRepository {

    private final SpringDataJpaSequenceInstanceRepository springDataJpaSequenceInstanceRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public long saveNewInstance(SequenceInstance sequenceInstance) {
        return insertInstance(sequenceInstance.getName(), sequenceInstance.getContextId(), sequenceInstance.getState().name(),
                sequenceInstance.getCreatedAt(), sequenceInstance.getRetainUntil());
    }

    private long insertInstance(String name, String contextId, String state, ZonedDateTime createdAt, ZonedDateTime retainUntil) {
        SequenceInstancePreparedStatementCreator psc = new SequenceInstancePreparedStatementCreator(name, contextId, state, createdAt, retainUntil);
        //noinspection DataFlowIssue
        return jdbcTemplate.query(psc, psc);
    }

    public Optional<Long> findIdByNameAndContextId(String name, String contextId) {
        return springDataJpaSequenceInstanceRepository.findIdByNameAndContextId(name, contextId);
    }

    public SequenceInstance getByIdAndLockForUpdate(long id, int idleLockTimeoutSeconds) {
        setIdleLockTimeoutForCurrentTransaction(idleLockTimeoutSeconds);
        return springDataJpaSequenceInstanceRepository.getByIdAndLockForUpdate(id);
    }

    /**
     * See <a href="https://www.postgresql.org/docs/current/sql-set.html">SET</a> and
     * <a href="https://www.postgresql.org/docs/current/runtime-config-client.html">Client Connection Settings</a>.
     * This ensures that the pessimistic locks for a sequence instance are held at most for the specified time
     * in case a connection is not property terminated.
     *
     * @param idleLockTimeoutSeconds Used to set `idle_in_transaction_session_timeout` (set to idleLockTimeoutSeconds * 1000 in milliseconds).
     *                               If < 0, the setting is not changed for the transaction, the system default of the
     *                               database server will be used instead.
     */
    private void setIdleLockTimeoutForCurrentTransaction(int idleLockTimeoutSeconds) {
        if (idleLockTimeoutSeconds < 0) {
            return;
        }

        int millis = idleLockTimeoutSeconds * 1000;
        String queryString = "SET LOCAL idle_in_transaction_session_timeout = '" + millis + "'";
        entityManager.createNativeQuery(queryString).executeUpdate();
    }

    /**
     * Deletes all CLOSED sequence instances
     *
     * @return The number of deleted sequence instances
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAllClosed() {
        return springDataJpaSequenceInstanceRepository.deleteAllClosed();
    }

    /**
     * Deletes all sequence instances that have expired (retain_until < cutoffTime).
     *
     * @param cutoffTime The timestamp to compare against for determining expiration
     * @return The number of deleted sequence instances
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteExpiredInstances(ZonedDateTime cutoffTime) {
        return springDataJpaSequenceInstanceRepository.deleteExpiredInstances(cutoffTime);
    }

}
