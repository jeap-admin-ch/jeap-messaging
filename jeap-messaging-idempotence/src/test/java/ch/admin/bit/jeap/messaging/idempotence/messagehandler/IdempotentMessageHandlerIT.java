package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.IdempotentProcessingJpaConfig;
import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.SpringDataJpaIdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.TestMessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.Ordered;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@Commit
@Transactional
@DataJpaTest
@ContextConfiguration(classes = {IdempotentMessageHandlerConfig.class, IdempotentProcessingJpaConfig.class, TestMessageHandler.class})
class IdempotentMessageHandlerIT {

    private static final String IDEMPOTENCE_ID = "idempotence-id";
    private static final String IDEMPOTENCE_ID_CONTEXT = "message-type-name";
    private static final String IDEMPOTENCE_ID_CONTEXT_V1 = "junitEvent";
    private static final String IDEMPOTENCE_ID_CONTEXT_V2 = "junitV2Event";
    private static final StringMessage MESSAGE = StringMessage.from("message", IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT);
    private static final StringMessage MESSAGE_V1 = StringMessage.from("message", IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT_V1);
    private static final StringMessage MESSAGE_V2 = StringMessage.from("message", IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT_V2);
    private static final String OTHER_IDEMPOTENCE_ID_CONTEXT = "message-type-name-other";
    private static final StringMessage MESSAGE_OTHER_CONTEXT = StringMessage.from("message", IDEMPOTENCE_ID, OTHER_IDEMPOTENCE_ID_CONTEXT);

    @Autowired
    IdempotentMessageHandlerAspect idempotentMessageHandlerAspect;

    @MockBean
    TestMessageHandler.Callback callback;

    @Autowired
    TestMessageHandler testMessageHandler;

    @Autowired
    SpringDataJpaIdempotentProcessingRepository springDataJpaIdempotentProcessingRepository;

    @BeforeEach
    void setUp() {
        assertNoIdempotentProcessing();
    }

    @AfterEach
    void tearDown() {
        springDataJpaIdempotentProcessingRepository.deleteByCreatedAtBefore(ZonedDateTime.now().plusSeconds(1));
    }

    @Test
    void testIdempotence_WhenHandleCalledTwiceWithSameMessage_ThenOnlyHandleOnce() {
        testMessageHandler.handleMessage(MESSAGE);
        TestTransaction.end();
        verify(callback, times(1)).handling(IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT);

        TestTransaction.start();
        testMessageHandler.handleMessage(MESSAGE);
        TestTransaction.end();
        verifyNoMoreInteractions(callback);
    }

    @Test
    void testIdempotence_WhenTransactionRolledBackForFirstHandle_ThenSecondAttemptHandledAgain() {
        testMessageHandler.handleMessage(MESSAGE);
        TestTransaction.flagForRollback();
        TestTransaction.end();

        verify(callback, times(1)).handling(IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT);
        TestTransaction.start();
        assertNoIdempotentProcessing();

        testMessageHandler.handleMessage(MESSAGE);
        TestTransaction.end();
        verify(callback, times(2)).handling(IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT);

        TestTransaction.start();
        testMessageHandler.handleMessage(MESSAGE);
        TestTransaction.end();
        verifyNoMoreInteractions(callback);
    }

    @Test
    void testIdempotence_WhenNoTransactionActive_ThenThrowsException() {
        TestTransaction.end();

        assertThatThrownBy(
                () -> testMessageHandler.handleMessage(MESSAGE)
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testIdempotence_WhenTwoMessagesShareTheSameIdempotenceIdButHaveDifferentContext_ThenBothHandledOnce() {
        testMessageHandler.handleMessage(MESSAGE);
        TestTransaction.end();
        verify(callback, times(1)).handling(IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT);

        TestTransaction.start();
        testMessageHandler.handleMessage(MESSAGE_OTHER_CONTEXT);
        TestTransaction.end();
        verify(callback, times(1)).handling(IDEMPOTENCE_ID, OTHER_IDEMPOTENCE_ID_CONTEXT);

        TestTransaction.start();
        testMessageHandler.handleMessage(MESSAGE_OTHER_CONTEXT);
        TestTransaction.end();
        TestTransaction.start();
        testMessageHandler.handleMessage(MESSAGE);
        TestTransaction.end();
        verifyNoMoreInteractions(callback);
    }

    @Test
    void testIdempotence_WhenTwoMessagesShareTheSameIdempotenceIdAndDifferentVersionsForSameContext_ThenSecondNotHandled() {
        testMessageHandler.handleMessage(MESSAGE_V1);
        TestTransaction.end();
        verify(callback, times(1)).handling(IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT_V1);

        TestTransaction.start();
        testMessageHandler.handleMessage(MESSAGE_V2);
        TestTransaction.end();
        verify(callback, never()).handling(IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT_V2);

        TestTransaction.start();
        testMessageHandler.handleMessage(MESSAGE_V2);
        TestTransaction.end();
        TestTransaction.start();
        testMessageHandler.handleMessage(MESSAGE_V1);
        TestTransaction.end();
        verifyNoMoreInteractions(callback);
    }

    @Test
    void testIdempotenceWithAdditionalParametersInSignature() {
        final int value = 42;
        final String text = "some-text";

        testMessageHandler.handleMessageWithAdditionalParameters(MESSAGE, value, text);
        TestTransaction.end();
        verify(callback, times(1)).handling(IDEMPOTENCE_ID, IDEMPOTENCE_ID_CONTEXT);

        TestTransaction.start();
        testMessageHandler.handleMessageWithAdditionalParameters(MESSAGE, value, text);
        TestTransaction.end();
        verifyNoMoreInteractions(callback);
    }

    private void assertNoIdempotentProcessing() {
        assertThat(springDataJpaIdempotentProcessingRepository.findAll()).isEmpty();
    }

    @Test
    void testIdempotentMessageHandlerAspectDefaultAdviceOrder() {
        assertThat(idempotentMessageHandlerAspect.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

}
