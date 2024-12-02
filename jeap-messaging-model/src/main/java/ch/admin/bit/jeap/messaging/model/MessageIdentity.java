package ch.admin.bit.jeap.messaging.model;

import java.time.*;

public interface MessageIdentity {
    String getId();

    String getIdempotenceId();

    Instant getCreated();

    default ZonedDateTime getCreatedZoned() {
        return ZonedDateTime.ofInstant(getCreated(), ZoneOffset.UTC);
    }

    default LocalDateTime getCreatedLocal() {
        return LocalDateTime.ofInstant(getCreated(), ZoneId.systemDefault());
    }
}
