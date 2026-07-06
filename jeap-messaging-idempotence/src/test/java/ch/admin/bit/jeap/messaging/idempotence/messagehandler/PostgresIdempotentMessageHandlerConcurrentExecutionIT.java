package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.IdempotentProcessingJpaConfig;
import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.JpaIdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.SpringDataJpaIdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.PostgresTestBase;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.SleepyHandlersConfig;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.SleepyTestMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Proves that on PostgreSQL, where the AUTO insert mode selects the 'INSERT ... ON CONFLICT DO NOTHING' insert,
 * handling the same message concurrently does not fail any of the handlers involved: the loser of the concurrent
 * idempotent processing insert waits for the winner's transaction to commit and then skips the message silently
 * instead of failing with a duplicate key violation.
 */
@Slf4j
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = {IdempotentMessageHandlerConfig.class, IdempotentProcessingJpaConfig.class, SleepyHandlersConfig.class})
class PostgresIdempotentMessageHandlerConcurrentExecutionIT extends PostgresTestBase {

    private static final StringMessage MESSAGE = StringMessage.from("message", "idempotence-id", "message-type-name");

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private SpringDataJpaIdempotentProcessingRepository springDataJpaIdempotentProcessingRepository;

    @Autowired
    private IdempotentProcessingRepository idempotentProcessingRepository;

    @Autowired
    private SleepyTestMessageHandler quickSleepyTestMessageHandler;

    @Autowired
    private SleepyTestMessageHandler slowSleepyTestMessageHandler;

    @BeforeEach
    void setUp() {
        assertThat(springDataJpaIdempotentProcessingRepository.findAll()).isEmpty();
    }

    @AfterEach
    void tearDown() {
        springDataJpaIdempotentProcessingRepository.deleteAll();
    }

    @Test
    void testIdempotence_WhenHandledConcurrentlyOnPostgres_ThenBothHandlersCompleteAndMessageIsHandledExactlyOnce() {
        // On PostgreSQL, the default AUTO insert mode must select the ON CONFLICT DO NOTHING insert
        assertThat(((JpaIdempotentProcessingRepository) idempotentProcessingRepository).isOnConflictDoNothingInsert())
                .isTrue();

        // Handle a message with the slow message handler in a separate thread and in its own transaction
        CompletableFuture<Void> slow = CompletableFuture.runAsync(() ->
                inNewTransaction(() -> slowSleepyTestMessageHandler.handleMessage(MESSAGE))
        );

        // Handle the same message with the quick message handler in a separate thread and in its own transaction
        CompletableFuture<Void> quick = CompletableFuture.runAsync(() ->
                inNewTransaction(() -> quickSleepyTestMessageHandler.handleMessage(MESSAGE))
        );

        // Neither handler must fail: the loser of the concurrent insert skips the message silently
        assertThatCode(() -> CompletableFuture.allOf(quick, slow).join()).doesNotThrowAnyException();
        assertThat(quick.isCompletedExceptionally()).isFalse();
        assertThat(slow.isCompletedExceptionally()).isFalse();

        // The message must have been processed by exactly one of the two handlers
        assertThat(quickSleepyTestMessageHandler.getExecutionCount() + slowSleepyTestMessageHandler.getExecutionCount())
                .isEqualTo(1);
        assertThat(springDataJpaIdempotentProcessingRepository.findAll()).hasSize(1);
        log.info("Executed handler: {}", quickSleepyTestMessageHandler.getExecutionCount() == 1 ? "quick" : "slow");
    }

}
