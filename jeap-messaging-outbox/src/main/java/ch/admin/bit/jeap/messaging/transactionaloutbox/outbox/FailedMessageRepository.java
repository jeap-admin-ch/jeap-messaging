package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import java.time.ZonedDateTime;
import java.util.List;

public interface FailedMessageRepository {

    int countFailedMessages(boolean resend);

    int countFailedMessages(ZonedDateTime failedStartingFrom, ZonedDateTime failedBefore, boolean resend);

    List<FailedMessage> findFailedMessages(ZonedDateTime failedStartingFrom, ZonedDateTime failedBefore, boolean resend, int maxNumMessagesToFind);

    List<FailedMessage> findFailedMessages(long afterId, ZonedDateTime failedBefore, boolean resend, int maxNumMessagesToFind);

}
