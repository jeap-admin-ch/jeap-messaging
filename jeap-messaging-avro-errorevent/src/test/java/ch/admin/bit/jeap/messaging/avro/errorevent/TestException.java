package ch.admin.bit.jeap.messaging.avro.errorevent;

import lombok.Getter;


@Getter
class TestException implements MessageHandlerExceptionInformation {
    private final String errorCode;
    private final Temporality temporality;
    private final String message;
    private final String description;
    private final String stackTraceAsString;

    @SuppressWarnings("SameParameterValue")
    TestException(String message, String stackTraceAsString, String errorCode, Temporality temporality) {
        this(message, stackTraceAsString, errorCode, temporality, null);
    }

    TestException(String message, String stackTraceAsString, String errorCode, Temporality temporality, String description) {
        this.message = message;
        this.stackTraceAsString = stackTraceAsString;
        this.errorCode = errorCode;
        this.temporality = temporality;
        this.description = description;
    }
}
