package ch.admin.bit.jeap.messaging.avro.errorevent;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageHandlerExceptionTest {

    private static final String ERROR_CODE = "errorCode";
    private static final String MESSAGE = "message";
    private static final String DESCRIPTION = "description";

    private static final String CAUSE_MESSAGE = "runtime exception cause";
    private static final Throwable CAUSE = new RuntimeException(CAUSE_MESSAGE);

    private static final MessageHandlerExceptionInformation.Temporality temporality = MessageHandlerExceptionInformation.Temporality.PERMANENT;

    @Test
    void minimal() {
        MessageHandlerException exception = MessageHandlerException.builder()
                .temporality(temporality)
                .message(MESSAGE)
                .errorCode(ERROR_CODE)
                .build();

        assertEquals(MessageHandlerExceptionInformation.Temporality.PERMANENT, exception.getTemporality());
        assertEquals(ERROR_CODE, exception.getErrorCode());
        assertThat(exception.getMessage(), containsString(MESSAGE));
        assertNull(exception.getDescription());
        assertNull(exception.getCause());

        final var stackTrace = exception.getStackTraceAsString();
        assertThat(stackTrace, containsString("MessageHandlerException"));
        assertThat(stackTrace, containsString(MESSAGE));

        assertThat(exception.toString(), containsString("ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException"));
        assertThat(exception.toString(), containsString(MESSAGE));
    }

    @Test
    void withCause() {
        MessageHandlerException exception = MessageHandlerException.builder()
                .temporality(temporality)
                .cause(CAUSE)
                .errorCode(ERROR_CODE)
                .build();

        assertEquals(MessageHandlerExceptionInformation.Temporality.PERMANENT, exception.getTemporality());
        assertEquals(ERROR_CODE, exception.getErrorCode());
        assertThat(exception.getMessage(), containsString(CAUSE_MESSAGE));
        assertNull(exception.getDescription());
        assertEquals(CAUSE, exception.getCause());
        assertEquals("java.lang.RuntimeException: " + CAUSE_MESSAGE, exception.getCause().toString());
        assertEquals(CAUSE_MESSAGE, exception.getCause().getMessage());

        final var stackTrace = exception.getStackTraceAsString();
        assertThat(stackTrace, containsString("MessageHandlerException"));
        assertThat(stackTrace, containsString(CAUSE_MESSAGE));

        assertThat(stackTrace, containsString("RuntimeException"));
        assertThat(stackTrace, containsString(CAUSE_MESSAGE));
    }

    @Test
    void withDescription() {
        MessageHandlerException exception = MessageHandlerException.builder()
                .temporality(temporality)
                .message(MESSAGE)
                .errorCode(ERROR_CODE)
                .description(DESCRIPTION)
                .build();

        assertEquals(MessageHandlerExceptionInformation.Temporality.PERMANENT, exception.getTemporality());
        assertEquals(ERROR_CODE, exception.getErrorCode());
        assertThat(exception.getMessage(), containsString(MESSAGE));
        assertEquals(DESCRIPTION, exception.getDescription());
        assertNull(exception.getCause());


        final var stackTrace = exception.getStackTraceAsString();
        assertThat(stackTrace, containsString("MessageHandlerException"));
        assertThat(stackTrace, containsString(MESSAGE));
    }

    @Test
    void withDescriptionAndCause() {
        MessageHandlerException exception = MessageHandlerException.builder()
                .temporality(temporality)
                .errorCode(ERROR_CODE)
                .description(DESCRIPTION)
                .cause(CAUSE)
                .build();

        assertEquals(MessageHandlerExceptionInformation.Temporality.PERMANENT, exception.getTemporality());
        assertEquals(ERROR_CODE, exception.getErrorCode());
        assertThat(exception.getMessage(), containsString(CAUSE_MESSAGE));
        assertEquals(DESCRIPTION, exception.getDescription());
        assertEquals(CAUSE, exception.getCause());
        assertEquals("java.lang.RuntimeException: " + CAUSE_MESSAGE, exception.getCause().toString());
        assertEquals(CAUSE_MESSAGE, exception.getCause().getMessage());


        final var stackTrace = exception.getStackTraceAsString();
        assertThat(stackTrace, containsString("MessageHandlerException"));
        assertThat(stackTrace, containsString(CAUSE_MESSAGE));

        assertThat(stackTrace, containsString("RuntimeException"));
        assertThat(stackTrace, containsString(CAUSE_MESSAGE));
    }
}
