package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.SpringDataJpaIdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.SleepyTestMessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Contract test for handling the same message concurrently while the idempotent processing records are created with
 * the portable 'INSERT ... WHERE NOT EXISTS' insert: exactly one of the two concurrent handlers must fail with an
 * {@link IdempotentMessageHandlerExecutionSkippedException} classified as a temporary error, and the message must
 * have been processed by exactly one handler. Implemented by the tests that run this contract against the different
 * databases supported by the idempotent message handler. Tests implementing this contract must run without a
 * test-managed transaction.
 */
public interface WhereNotExistsInsertConcurrentExecutionContract {

    StringMessage MESSAGE = StringMessage.from("message", "idempotence-id", "message-type-name");

    SleepyTestMessageHandler getQuickSleepyTestMessageHandler();

    SleepyTestMessageHandler getSlowSleepyTestMessageHandler();

    SpringDataJpaIdempotentProcessingRepository getSpringDataJpaIdempotentProcessingRepository();

    void inNewTransaction(Runnable runnable);

    @BeforeEach
    default void assertNoIdempotentProcessingRecords() {
        assertThat(getSpringDataJpaIdempotentProcessingRepository().findAll()).isEmpty();
    }

    @AfterEach
    default void deleteAllIdempotentProcessingRecords() {
        getSpringDataJpaIdempotentProcessingRepository().deleteAll();
    }

    @Test
    default void testIdempotence_WhenHandledConcurrently_ThenOnlyOneHandlerCompletesWithoutException() {
        // Handle a message with the slow message handler in a separate thread and in its own transaction
        CompletableFuture<Void> slow = CompletableFuture.runAsync(() ->
                inNewTransaction(() -> getSlowSleepyTestMessageHandler().handleMessage(MESSAGE))
        );

        // Handle the same message with the quick message handler in a separate thread and in its own transaction
        CompletableFuture<Void> quick = CompletableFuture.runAsync(() ->
                inNewTransaction(() -> getQuickSleepyTestMessageHandler().handleMessage(MESSAGE))
        );

        // One handler should fail because idempotent processing should prevent that both handlers complete and commit
        assertThatThrownBy(() -> CompletableFuture.allOf(quick, slow).join())
                .hasMessageContaining("Skipping processing for the message context 'message-type-name' with idempotence id 'idempotence-id'");

        // Only one of the handlers should have failed, and the loser must not have executed the message handler
        assertThat(quick.isCompletedExceptionally() != slow.isCompletedExceptionally()).isTrue();
        assertThat(getQuickSleepyTestMessageHandler().getExecutionCount() + getSlowSleepyTestMessageHandler().getExecutionCount())
                .isEqualTo(1);
        assertThat(getSpringDataJpaIdempotentProcessingRepository().findAll()).hasSize(1);

        // The failed handler must have failed with an exception that the error handling classifies as a
        // temporary error eligible for an automatic resend (instead of a manual task)
        CompletableFuture<Void> failed = quick.isCompletedExceptionally() ? quick : slow;
        Throwable cause = catchThrowable(failed::join).getCause();
        assertThat(cause).isInstanceOf(IdempotentMessageHandlerExecutionSkippedException.class);
        MessageHandlerExceptionInformation exceptionInformation = (MessageHandlerExceptionInformation) cause;
        assertThat(exceptionInformation.getTemporality()).isEqualTo(MessageHandlerExceptionInformation.Temporality.TEMPORARY);
        assertThat(exceptionInformation.getErrorCode())
                .isEqualTo(IdempotentMessageHandlerExecutionSkippedException.ERROR_CODE_CONCURRENT_HANDLING);
    }

}
