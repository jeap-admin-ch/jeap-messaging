package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequenceInstance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Optional;

@Repository
interface SpringDataJpaSequenceInstanceRepository extends JpaRepository<SequenceInstance, Long> {

    @Query(nativeQuery = true, value = "SELECT id FROM sequence_instance WHERE name = ?1 AND context_id = ?2")
    Optional<Long> findIdByNameAndContextId(String name, String contextId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT si FROM SequenceInstance si WHERE si.id = ?1")
    SequenceInstance getByIdAndLockForUpdate(long sequenceInstanceId);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM sequence_instance WHERE state = 'CLOSED'")
    int deleteAllClosed();

    /**
     * Delete all expired sequence instances (retain_until < cutoffTime)
     *
     * @param cutoffTime The timestamp to compare against for determining expiration
     * @return The number of deleted sequence instances
     */
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM sequence_instance WHERE retain_until < ?1")
    int deleteExpiredInstances(ZonedDateTime cutoffTime);
}
