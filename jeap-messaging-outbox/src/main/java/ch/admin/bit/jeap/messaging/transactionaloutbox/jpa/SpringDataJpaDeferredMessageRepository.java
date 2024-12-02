package ch.admin.bit.jeap.messaging.transactionaloutbox.jpa;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessage;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.SendFailureReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
@Repository
interface SpringDataJpaDeferredMessageRepository extends JpaRepository<DeferredMessage, Long> {

    String READY_TO_BE_SENT_CONDITION = "sent_immediately IS NULL and sent_scheduled IS NULL AND failed IS NULL AND " +
            "(send_immediately = false OR (send_immediately = true AND CURRENT_TIMESTAMP > schedule_after)) " +
            "OR resend = true";

    String FROM_SENT_IMMEDIATELY_IS_NULL_AND_SENT_SCHEDULED_IS_NULL_AND_CREATED_BEFORE = "FROM deferred_message d " +
            "WHERE d.sent_immediately IS NULL " +
            "AND d.sent_scheduled IS NULL " +
            "AND d.created < :dateTime ";

    String FROM_SENT_IMMEDIATELY_BEFORE_OR_SCHEDULED_BEFORE = "FROM deferred_message d " +
            "WHERE d.sent_immediately < :sentImmediatelyBefore " +
            "OR d.sent_scheduled < :sentScheduledBefore ";

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("UPDATE DeferredMessage m SET m.sentImmediately = :sentTime WHERE m.id = :id")
    int markSentImmediately(@Param("id") long id, @Param("sentTime") ZonedDateTime sentTime);

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("UPDATE DeferredMessage m SET m.sentScheduled = :sentTime, m.failed = null, m.resend = false WHERE m.id = :id")
    int markSentScheduled(@Param("id") long id, @Param("sentTime") ZonedDateTime sentTime);

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("UPDATE DeferredMessage m SET m.failed = :failedTime, m.failReason = :failReason, m.resend = false WHERE m.id = :id")
    int markFailed(@Param("id") long id, @Param("failedTime") ZonedDateTime failedTime, @Param("failReason") SendFailureReason failReason);

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("UPDATE DeferredMessage m SET m.resend = :resend WHERE m.id = :id")
    int markForResend(@Param("id") long id, @Param("resend") boolean resend);

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("UPDATE DeferredMessage m SET m.scheduleAfter = :scheduleAfter WHERE m.id = :id")
    int setScheduleAfter(@Param("id") long id, @Param("scheduleAfter") ZonedDateTime scheduleAfter);

    @Transactional(readOnly = true)
    @Query(nativeQuery = true, value = "SELECT * FROM deferred_message WHERE " + READY_TO_BE_SENT_CONDITION + " order by id limit :numMessages")
    List<DeferredMessage> findMessagesReadyToBeSent(@Param("numMessages") int numMessages);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE " + FROM_SENT_IMMEDIATELY_BEFORE_OR_SCHEDULED_BEFORE)
    void deleteBySentImmediatelyBeforeOrSentScheduledBefore(@Param("sentImmediatelyBefore") ZonedDateTime sentImmediatelyBefore, @Param("sentScheduledBefore") ZonedDateTime sentScheduledBefore);

    @Transactional(readOnly = true)
    @Query(nativeQuery = true, value = "SELECT count(*) " + FROM_SENT_IMMEDIATELY_BEFORE_OR_SCHEDULED_BEFORE)
    int countSentImmediatelyBeforeOrSentScheduledBefore(@Param("sentImmediatelyBefore") ZonedDateTime sentImmediatelyBefore, @Param("sentScheduledBefore") ZonedDateTime sentScheduledBefore);

    @Transactional
    @Modifying
    @Query(nativeQuery = true,
            value = "DELETE " + FROM_SENT_IMMEDIATELY_IS_NULL_AND_SENT_SCHEDULED_IS_NULL_AND_CREATED_BEFORE)
    void deleteBySentImmediatelyIsNullAndSentScheduledIsNullAndCreatedBefore(@Param("dateTime") ZonedDateTime dateTime);

    @Transactional(readOnly = true)
    @Query(nativeQuery = true,
            value = "select count(*) " + FROM_SENT_IMMEDIATELY_IS_NULL_AND_SENT_SCHEDULED_IS_NULL_AND_CREATED_BEFORE)
    int countSentImmediatelyIsNullAndSentScheduledIsNullAndCreatedBefore(@Param("dateTime") ZonedDateTime dateTime);

    @Transactional(readOnly = true)
    @Query(nativeQuery = true, value = "SELECT COUNT (*) FROM deferred_message WHERE " + READY_TO_BE_SENT_CONDITION)
    int countMessagesReadyToBeSent();

    @Transactional(readOnly = true)
    int countByFailedIsNotNullAndResend(boolean resend);

    @Transactional(readOnly = true)
    @Query("SELECT COUNT(d) FROM DeferredMessage d WHERE d.failed IS NOT NULL AND d.failed >= :failedStartingFrom AND d.failed < :failedBefore AND d.resend = :resend")
    int countFailedBetween(@Param("failedStartingFrom") ZonedDateTime failedStartingFrom, @Param("failedBefore") ZonedDateTime failedBefore, @Param("resend") boolean resend);

}
