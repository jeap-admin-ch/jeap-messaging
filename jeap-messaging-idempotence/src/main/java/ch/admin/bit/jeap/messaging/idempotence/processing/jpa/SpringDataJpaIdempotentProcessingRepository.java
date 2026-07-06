package ch.admin.bit.jeap.messaging.idempotence.processing.jpa;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessing;
import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Repository
public interface SpringDataJpaIdempotentProcessingRepository extends JpaRepository<IdempotentProcessing, IdempotentProcessingIdentity> {

    String FROM_IDEMPOTENT_PROCESSING_WHERE_CREATED_AT_BEFORE = " FROM idempotent_processing ip WHERE ip.created_at < :createdBefore";

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "INSERT INTO idempotent_processing (idempotence_id, idempotence_id_context, created_at) " +
                                       "SELECT ?1, ?2, ?3 WHERE NOT EXISTS " +
                                       "(SELECT * FROM idempotent_processing WHERE idempotence_id = ?1 AND idempotence_id_context = ?2)")
    int createIfNotExists(String idempotenceId, String idempotenceIdContext, ZonedDateTime createdAt);

    // A record concurrently inserted by another transaction fails createIfNotExists() with a unique constraint
    // violation once the other transaction commits. ON CONFLICT DO NOTHING instead skips the insert without an error
    // in this case (guaranteed under read committed isolation). The conflict target is omitted deliberately: the
    // primary key is the table's only unique constraint, and the target-less form is also supported by
    // PostgreSQL-compatible databases (e.g. H2 in PostgreSQL compatibility mode).
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "INSERT INTO idempotent_processing (idempotence_id, idempotence_id_context, created_at) " +
                                       "VALUES (?1, ?2, ?3) " +
                                       "ON CONFLICT DO NOTHING")
    int createIfNotExistsOnConflictDoNothing(String idempotenceId, String idempotenceIdContext, ZonedDateTime createdAt);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE" + FROM_IDEMPOTENT_PROCESSING_WHERE_CREATED_AT_BEFORE)
    void deleteByCreatedAtBefore(@Param("createdBefore") ZonedDateTime createdBefore);

    @Transactional(readOnly = true)
    @Query(nativeQuery = true, value = "SELECT count(*)" + FROM_IDEMPOTENT_PROCESSING_WHERE_CREATED_AT_BEFORE)
    int countCreatedAtBefore(@Param("createdBefore") ZonedDateTime createdBefore);

}
