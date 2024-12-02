package ch.admin.bit.jeap.messaging.transactionaloutbox.jpa;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.*;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.ZonedDateTime;
import java.util.List;

import static ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxMetrics.*;

@Repository
@RequiredArgsConstructor
class JpaDeferredMessageRepository implements DeferredMessageRepository, FailedMessageRepository {

    private static final String FAILED_MESSAGE_CONSTRUCTOR_EXPRESSION = "SELECT NEW ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.FailedMessage(" +
            "d.id, d.topic, d.messageId, d.messageIdempotenceId, d.messageTypeName, d.created, d.failed, d.failReason, d.resend) ";
    private static final String FIND_FAILED_MESSAGES_BETWEEN_INSTANTS = FAILED_MESSAGE_CONSTRUCTOR_EXPRESSION +
            "FROM DeferredMessage d WHERE d.failed IS NOT NULL AND d.failed >= :failedStartingFrom AND d.failed < :failedBefore AND d.resend = :resend ORDER BY d.id";
    private static final String FIND_FAILED_MESSAGES_STARTING_AFTER_ID = FAILED_MESSAGE_CONSTRUCTOR_EXPRESSION +
            "FROM DeferredMessage d WHERE d.failed IS NOT NULL AND d.id > :afterId AND d.failed < :failedBefore AND d.resend = :resend ORDER BY d.id";

    private final SpringDataJpaDeferredMessageRepository springDataJpaDeferredMessageRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public DeferredMessage getById(long id) {
        return springDataJpaDeferredMessageRepository.getReferenceById(id);
    }

    @Override
    public DeferredMessage save(DeferredMessage deferredMessage) {
        return springDataJpaDeferredMessageRepository.save(deferredMessage);
    }

    @Override
    public void deleteById(long id) {
        springDataJpaDeferredMessageRepository.deleteById(id);
    }

    @Override
    public void markSentImmediately(long id, ZonedDateTime sentTime) {
        if (springDataJpaDeferredMessageRepository.markSentImmediately(id, sentTime) == 0) {
            throw TransactionalOutboxException.deferredMessageNotFoundInOutbox(id);
        }
    }

    @Override
    public void markSentScheduled(long id, ZonedDateTime sentTime) {
        if (springDataJpaDeferredMessageRepository.markSentScheduled(id, sentTime) == 0) {
            throw TransactionalOutboxException.deferredMessageNotFoundInOutbox(id);
        }
    }

    @Override
    public void markFailed(long id, ZonedDateTime failedTime, SendFailureReason failReason) {
        if (springDataJpaDeferredMessageRepository.markFailed(id, failedTime, failReason) == 0) {
            throw TransactionalOutboxException.deferredMessageNotFoundInOutbox(id);
        }
    }

    @Override
    public void markForResend(long id, boolean resend) {
        if (springDataJpaDeferredMessageRepository.markForResend(id, resend) == 0) {
            throw TransactionalOutboxException.deferredMessageNotFoundInOutbox(id);
        }
    }

    @Override
    public void setScheduleAfter(long id, ZonedDateTime scheduleAfter) {
        if (springDataJpaDeferredMessageRepository.setScheduleAfter(id, scheduleAfter) == 0) {
            throw TransactionalOutboxException.deferredMessageNotFoundInOutbox(id);
        }
    }

    @Override
    @Timed(value = MESSAGES_READY_TO_BE_SENT_TIMER, description = "Search messages ready to be sent.")
    public List<DeferredMessage> findMessagesReadyToBeSent(int numMessages) {
        return springDataJpaDeferredMessageRepository.findMessagesReadyToBeSent(numMessages);
    }

    @Override
    public int deleteMessagesSentBefore(ZonedDateTime dateTime) {
        int deleteCount = springDataJpaDeferredMessageRepository.countSentImmediatelyBeforeOrSentScheduledBefore(dateTime, dateTime);
        springDataJpaDeferredMessageRepository.deleteBySentImmediatelyBeforeOrSentScheduledBefore(dateTime, dateTime);
        return deleteCount;
    }

    @Override
    public int deleteUnsentMessagesCreatedBefore(ZonedDateTime dateTime) {
        int deleteCount = springDataJpaDeferredMessageRepository.countSentImmediatelyIsNullAndSentScheduledIsNullAndCreatedBefore(dateTime);
        springDataJpaDeferredMessageRepository.deleteBySentImmediatelyIsNullAndSentScheduledIsNullAndCreatedBefore(dateTime);
        return deleteCount;
    }

    @Override
    public int countMessagesReadyToBeSent() {
        return springDataJpaDeferredMessageRepository.countMessagesReadyToBeSent();
    }

    @Override
    public int countFailedMessages(boolean resend) {
        return springDataJpaDeferredMessageRepository.countByFailedIsNotNullAndResend(resend);
    }

    @Override
    public int countFailedMessages(ZonedDateTime failedStartingFrom, ZonedDateTime failedBefore, boolean resend) {
        return springDataJpaDeferredMessageRepository.countFailedBetween(failedStartingFrom, failedBefore, resend);
    }

    @Transactional(readOnly = true)
    @Override
    public List<FailedMessage> findFailedMessages(ZonedDateTime failedStartingFrom, ZonedDateTime failedBefore, boolean resend, int maxNumMessagesToFind) {
        TypedQuery<FailedMessage> query = entityManager.createQuery(FIND_FAILED_MESSAGES_BETWEEN_INSTANTS, FailedMessage.class);
        query.setParameter("failedStartingFrom", failedStartingFrom);
        query.setParameter("failedBefore", failedBefore);
        query.setParameter("resend", resend);
        query.setMaxResults(maxNumMessagesToFind);
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<FailedMessage> findFailedMessages(long afterId, ZonedDateTime failedBefore, boolean resend, int maxNumMessagesToFind) {
        TypedQuery<FailedMessage> query = entityManager.createQuery(FIND_FAILED_MESSAGES_STARTING_AFTER_ID, FailedMessage.class);
        query.setParameter("afterId", afterId);
        query.setParameter("failedBefore", failedBefore);
        query.setParameter("resend", resend);
        query.setMaxResults(maxNumMessagesToFind);
        return query.getResultList();
    }

    @Override
    public List<DeferredMessage> findAll() {
        return springDataJpaDeferredMessageRepository.findAll();
    }

}
