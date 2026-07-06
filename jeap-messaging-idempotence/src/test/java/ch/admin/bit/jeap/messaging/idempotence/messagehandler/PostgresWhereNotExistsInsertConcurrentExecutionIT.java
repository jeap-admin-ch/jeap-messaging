package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.IdempotentProcessingJpaConfig;
import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.JpaIdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.SpringDataJpaIdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.PostgresTestBase;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.SleepyHandlersConfig;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.SleepyTestMessageHandler;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the concurrent execution contract test against a real PostgreSQL database with the insert mode explicitly
 * forced to the portable 'INSERT ... WHERE NOT EXISTS' insert, proving that on PostgreSQL the previous behavior is
 * retained when the ON CONFLICT DO NOTHING insert is not used.
 */
@DataJpaTest(properties = "jeap.messaging.idempotent-processing.insert-mode=where-not-exists")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = {IdempotentMessageHandlerConfig.class, IdempotentProcessingJpaConfig.class, SleepyHandlersConfig.class})
class PostgresWhereNotExistsInsertConcurrentExecutionIT extends PostgresTestBase implements WhereNotExistsInsertConcurrentExecutionContract {

    @Getter
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private SpringDataJpaIdempotentProcessingRepository springDataJpaIdempotentProcessingRepository;

    @Autowired
    private IdempotentProcessingRepository idempotentProcessingRepository;

    @Getter
    @Autowired
    private SleepyTestMessageHandler quickSleepyTestMessageHandler;

    @Getter
    @Autowired
    private SleepyTestMessageHandler slowSleepyTestMessageHandler;

    @Test
    void testInsertModeConfiguration_WhenWhereNotExistsConfigured_ThenOverridesAutoDetection() {
        assertThat(((JpaIdempotentProcessingRepository) idempotentProcessingRepository).isOnConflictDoNothingInsert())
                .isFalse();
    }

}
