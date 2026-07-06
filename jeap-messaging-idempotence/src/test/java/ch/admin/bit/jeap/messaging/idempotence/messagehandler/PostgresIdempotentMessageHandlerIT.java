package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.IdempotentProcessingJpaConfig;
import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.JpaIdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.SpringDataJpaIdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.PostgresTestBase;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.TestMessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Proves that on PostgreSQL, where the AUTO insert mode selects the 'INSERT ... ON CONFLICT DO NOTHING' insert,
 * the base guarantees of the idempotent message handler are preserved: a message is handled only once, and a
 * message whose processing transaction was rolled back is handled again on the next attempt.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = {IdempotentMessageHandlerConfig.class, IdempotentProcessingJpaConfig.class, TestMessageHandler.class})
class PostgresIdempotentMessageHandlerIT extends PostgresTestBase {

    private static final String IDEMPOTENCE_ID = "idempotence-id";
    private static final String IDEMPOTENCE_ID_CONTEXT = "message-type-name";
    private static final StringMessage MESSAGE = StringMessage.from("message", IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT);

    @MockitoBean
    TestMessageHandler.Callback callback;

    @Autowired
    TestMessageHandler testMessageHandler;

    @Autowired
    IdempotentProcessingRepository idempotentProcessingRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    SpringDataJpaIdempotentProcessingRepository springDataJpaIdempotentProcessingRepository;

    @BeforeEach
    void setUp() {
        assertThat(springDataJpaIdempotentProcessingRepository.findAll()).isEmpty();
    }

    @AfterEach
    void tearDown() {
        springDataJpaIdempotentProcessingRepository.deleteAll();
    }

    @Test
    void testInsertModeAutoDetection_WhenOnPostgres_ThenOnConflictDoNothingInsertSelected() {
        assertThat(((JpaIdempotentProcessingRepository) idempotentProcessingRepository).isOnConflictDoNothingInsert())
                .isTrue();
    }

    @Test
    void testIdempotence_WhenHandleCalledTwiceWithSameMessage_ThenOnlyHandleOnce() {
        inNewTransaction(() -> testMessageHandler.handleMessage(MESSAGE));
        verify(callback, times(1)).handling(IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT);

        inNewTransaction(() -> testMessageHandler.handleMessage(MESSAGE));
        verifyNoMoreInteractions(callback);
        assertThat(springDataJpaIdempotentProcessingRepository.findAll()).hasSize(1);
    }

    @Test
    void testIdempotence_WhenTransactionRolledBackForFirstHandle_ThenSecondAttemptHandledAgain() {
        assertThatThrownBy(() -> inNewTransaction(() -> {
            testMessageHandler.handleMessage(MESSAGE);
            throw new IllegalStateException("Simulating a message processing failure");
        })).isInstanceOf(IllegalStateException.class);

        // The handler was executed, but the idempotent processing record was rolled back with the transaction
        verify(callback, times(1)).handling(IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT);
        assertThat(springDataJpaIdempotentProcessingRepository.findAll()).isEmpty();

        // The message must be handled again on the next attempt
        inNewTransaction(() -> testMessageHandler.handleMessage(MESSAGE));
        verify(callback, times(2)).handling(IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT);

        // But not once more after the successful attempt
        inNewTransaction(() -> testMessageHandler.handleMessage(MESSAGE));
        verifyNoMoreInteractions(callback);
    }

}
