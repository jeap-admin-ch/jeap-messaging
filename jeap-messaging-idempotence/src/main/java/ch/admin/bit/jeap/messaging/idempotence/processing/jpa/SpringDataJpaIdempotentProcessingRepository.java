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
    // With PostgreSQL a simpler query would be possible: INSERT INTO idempotent_processing (idempotence_id, idempotence_id_context, created_at) VALUES ( ?1, ?2, ?3) ON CONFLICT DO NOTHING
    @Query(nativeQuery = true, value = "INSERT INTO idempotent_processing (idempotence_id, idempotence_id_context, created_at) " +
                                       "SELECT ?1, ?2, ?3 WHERE NOT EXISTS " +
                                       "(SELECT * FROM idempotent_processing WHERE idempotence_id = ?1 AND idempotence_id_context = ?2)")
    int createIfNotExists(String idempotenceId, String idempotenceIdContext, ZonedDateTime createdAt);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE" + FROM_IDEMPOTENT_PROCESSING_WHERE_CREATED_AT_BEFORE)
    void deleteByCreatedAtBefore(@Param("createdBefore") ZonedDateTime createdBefore);

    @Transactional(readOnly = true)
    @Query(nativeQuery = true, value = "SELECT count(*)" + FROM_IDEMPOTENT_PROCESSING_WHERE_CREATED_AT_BEFORE)
    int countCreatedAtBefore(@Param("createdBefore") ZonedDateTime createdBefore);

}
