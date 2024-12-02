package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport;

import ch.admin.bit.jeap.messaging.model.MessagePayload;
import lombok.Value;

@Value
public class StringMessagePayload implements MessagePayload {

    private final String message;

}
