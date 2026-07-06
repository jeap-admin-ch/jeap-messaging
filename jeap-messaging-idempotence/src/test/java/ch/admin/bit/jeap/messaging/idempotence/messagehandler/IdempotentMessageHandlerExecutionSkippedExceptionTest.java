package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import static ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandlerExecutionSkippedException.concurrentHandlingIdempotentProcessingException;
import static ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandlerExecutionSkippedException.generalIdempotentProcessingException;
import static org.assertj.core.api.Assertions.assertThat;

class IdempotentMessageHandlerExecutionSkippedExceptionTest {

    private static final IdempotentProcessingIdentity IDENTITY =
            IdempotentProcessingIdentity.from("idempotence-id", "message-type-name");

    // The aspect creates the concurrent handling exception for causes signalling a conflicting concurrent insert
    private static final Throwable CONCURRENT_HANDLING_CAUSE = new DataIntegrityViolationException(
            "ERROR: duplicate key value violates unique constraint \"pk_idempotent_processing\"");

    // The aspect creates the general exception for any other cause, e.g. the database being unavailable
    private static final Throwable GENERAL_CAUSE = new DataAccessResourceFailureException(
            "Failed to obtain JDBC Connection");

    @Test
    void testConcurrentHandlingException_ProvidesTemporaryMessageHandlerExceptionInformation() {
        IdempotentMessageHandlerExecutionSkippedException exception =
                concurrentHandlingIdempotentProcessingException(IDENTITY, CONCURRENT_HANDLING_CAUSE);

        assertThat(exception).isInstanceOf(MessageHandlerExceptionInformation.class);
        assertThat(exception.getTemporality()).isEqualTo(MessageHandlerExceptionInformation.Temporality.TEMPORARY);
        assertThat(exception.getErrorCode())
                .isEqualTo(IdempotentMessageHandlerExecutionSkippedException.ERROR_CODE_CONCURRENT_HANDLING);
        assertThat(exception.getMessage())
                .contains("message context 'message-type-name'")
                .contains("idempotence id 'idempotence-id'")
                .contains("handled concurrently");
        assertThat(exception.getDescription()).isNotBlank();
        assertThat(exception.getStackTraceAsString())
                .contains("DataIntegrityViolationException")
                .contains("duplicate key value violates unique constraint");
        assertThat(exception.getCause()).isSameAs(CONCURRENT_HANDLING_CAUSE);
    }

    @Test
    void testGeneralException_ProvidesTemporaryMessageHandlerExceptionInformation() {
        IdempotentMessageHandlerExecutionSkippedException exception =
                generalIdempotentProcessingException(IDENTITY, GENERAL_CAUSE);

        assertThat(exception).isInstanceOf(MessageHandlerExceptionInformation.class);
        assertThat(exception.getTemporality()).isEqualTo(MessageHandlerExceptionInformation.Temporality.TEMPORARY);
        assertThat(exception.getErrorCode())
                .isEqualTo(IdempotentMessageHandlerExecutionSkippedException.ERROR_CODE_PROCESSING_FAILED);
        assertThat(exception.getMessage())
                .contains("message context 'message-type-name'")
                .contains("idempotence id 'idempotence-id'")
                .contains("unexpected exception");
        assertThat(exception.getDescription()).isNotBlank();
        assertThat(exception.getStackTraceAsString())
                .contains("DataAccessResourceFailureException")
                .contains("Failed to obtain JDBC Connection");
        assertThat(exception.getCause()).isSameAs(GENERAL_CAUSE);
    }

}
