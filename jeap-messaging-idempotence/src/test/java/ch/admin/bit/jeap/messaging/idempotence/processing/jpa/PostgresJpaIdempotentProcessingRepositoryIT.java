package ch.admin.bit.jeap.messaging.idempotence.processing.jpa;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.PostgresTestBase;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the idempotent processing repository contract tests against a real PostgreSQL database, on which the AUTO
 * insert mode selects the PostgreSQL-specific 'INSERT ... ON CONFLICT DO NOTHING' insert.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = IdempotentProcessingJpaConfig.class)
class PostgresJpaIdempotentProcessingRepositoryIT extends PostgresTestBase implements IdempotentProcessingRepositoryContract {

    @Getter
    @Autowired
    private IdempotentProcessingRepository repository;

    @Getter
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private SpringDataJpaIdempotentProcessingRepository springDataRepository;

    @Test
    void testInsertModeAutoDetection_WhenOnPostgres_ThenOnConflictDoNothingInsertSelected() {
        assertThat(((JpaIdempotentProcessingRepository) repository).isOnConflictDoNothingInsert()).isTrue();
    }

}
