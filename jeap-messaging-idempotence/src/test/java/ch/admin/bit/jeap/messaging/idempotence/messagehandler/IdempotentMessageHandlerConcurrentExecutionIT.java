package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.IdempotentProcessingJpaConfig;
import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.SpringDataJpaIdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.SleepyHandlersConfig;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.SleepyTestMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@DataJpaTest
@ContextConfiguration(classes = {IdempotentMessageHandlerConfig.class, IdempotentProcessingJpaConfig.class, SleepyHandlersConfig.class})
class IdempotentMessageHandlerConcurrentExecutionIT {

    private static final StringMessage MESSAGE = StringMessage.from("message", "idempotence-id", "message-type-name");

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private SpringDataJpaIdempotentProcessingRepository springDataJpaIdempotentProcessingRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SleepyTestMessageHandler quickSleepyTestMessageHandler;

    @Autowired
    private SleepyTestMessageHandler slowSleepyTestMessageHandler;


    @BeforeEach
    void setUp() {
        assertNoIdempotentProcessing();
    }

    @AfterEach
    void tearDown() {
        springDataJpaIdempotentProcessingRepository.deleteByCreatedAtBefore(ZonedDateTime.now().plusSeconds(1));
    }

    @Test
    void testIdempotence_WhenHandledConcurrently_ThenOnlyOneHandlerCompletesWithoutException() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());

        // Handle a message with the slow message handler in a separate thread and in its own transaction
        CompletableFuture<Void> slow = CompletableFuture.runAsync( () ->
            transactionTemplate.executeWithoutResult(status -> slowSleepyTestMessageHandler.handleMessage(MESSAGE))
        );

        // Handle the same message with the quick message handler in a separate thread and in its own transaction
        CompletableFuture<Void> quick = CompletableFuture.runAsync( () ->
            transactionTemplate.executeWithoutResult(status -> quickSleepyTestMessageHandler.handleMessage(MESSAGE))
        );

        // One handler should fail because idempotent processing should prevent that both handlers complete and commit
        assertThatThrownBy( () -> CompletableFuture.allOf(quick, slow).join())
                .hasMessageContaining("Skipping processing for the message context 'message-type-name' with idempotence id 'idempotence-id'");

        // Only one of the handlers should have failed
        assertThat(quick.isCompletedExceptionally() != slow.isCompletedExceptionally()).isTrue();
        log.info("Failed handler: {}", quick.isCompletedExceptionally() ? "quick" : "slow");
    }

    private void assertNoIdempotentProcessing() {
        assertThat(springDataJpaIdempotentProcessingRepository.findAll()).isEmpty();
    }

}
