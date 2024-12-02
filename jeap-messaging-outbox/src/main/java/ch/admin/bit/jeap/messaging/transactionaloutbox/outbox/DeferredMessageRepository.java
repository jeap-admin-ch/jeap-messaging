package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import java.time.ZonedDateTime;
import java.util.List;

public interface DeferredMessageRepository {

    DeferredMessage getById(long id);

    DeferredMessage save(DeferredMessage deferredMessage);

    void deleteById(long id);

    void markSentImmediately(long id, ZonedDateTime sentTime);

    void markSentScheduled(long id, ZonedDateTime sentTime);

    void markFailed(long id, ZonedDateTime failedTime, SendFailureReason failReason);

    void markForResend(long id, boolean resend);

    void setScheduleAfter(long id, ZonedDateTime scheduleAfter);

    List<DeferredMessage> findMessagesReadyToBeSent(int numMessages);

    int deleteMessagesSentBefore(ZonedDateTime timestamp);

    int deleteUnsentMessagesCreatedBefore(ZonedDateTime timestamp);

    int countMessagesReadyToBeSent();

    List<DeferredMessage> findAll();

}
