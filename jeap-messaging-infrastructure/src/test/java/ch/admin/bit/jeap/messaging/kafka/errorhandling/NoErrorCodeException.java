package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import lombok.Getter;

@Getter
public class NoErrorCodeException extends RuntimeException implements MessageHandlerExceptionInformation {

    private final String errorCode;
    private final Temporality temporality;

    public NoErrorCodeException(String errorCode, Temporality temporality) {
        this.errorCode = errorCode;
        this.temporality = temporality;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getStackTraceAsString() {
        return null;
    }
}
