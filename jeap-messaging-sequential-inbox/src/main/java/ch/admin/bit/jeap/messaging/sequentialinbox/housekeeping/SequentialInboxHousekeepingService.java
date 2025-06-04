package ch.admin.bit.jeap.messaging.sequentialinbox.housekeeping;

import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.MessageRepository;
import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.SequenceInstanceRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

/**
 * Service responsible for performing housekeeping tasks for the sequential inbox.
 * This includes:
 * - Removing expired messages that are past their retention period
 * - Cleaning up closed sequence instances
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SequentialInboxHousekeepingService {

    private final MessageRepository messageRepository;
    private final SequenceInstanceRepository sequenceInstanceRepository;

    /**
     * Scheduled task that runs every 15min by default to remove expired messages and sequence instances
     * from the database. Messages and sequence instances are considered expired when their
     * retain_until timestamp is in the past. A single timestamp is used as the cutoff time for all operations
     * to prevent race conditions.
     */
    @Scheduled(cron = "${jeap.messaging.sequential-inbox.housekeeping.expiry-cron:0 */15 * * * *}")
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    @Timed(value = "jeap.messaging.sequential-inbox.housekeeping.expired")
    @SchedulerLock(name = "sequential-inbox-housekeeping-expired", lockAtLeastFor = "5s", lockAtMostFor = "1h")
    public void deleteExpiredMessages() {
        log.debug("Starting housekeeping task: deleting expired messages and sequence instances");

        // Use a single timestamp for all operations to avoid race conditions
        ZonedDateTime now = ZonedDateTime.now();

        // Delete in order to respect foreign key constraints
        int messagesDeleted = messageRepository.deleteExpiredMessages(now);
        int instancesDeleted = sequenceInstanceRepository.deleteExpiredInstances(now);

        log.info("Sequential inbox housekeeping task completed: deleted {} expired messages and {} expired sequence instances",
                messagesDeleted, instancesDeleted);
    }

    /**
     * Scheduled task that runs every 15min (starting at *:00 + 5min) by default to clean up sequence instances
     * that are in CLOSED state.
     */
    @Scheduled(cron = "${jeap.messaging.sequential-inbox.housekeeping.closed-instances-cron:0 5/15 * * * *}")
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    @Timed(value = "jeap.messaging.sequential-inbox.housekeeping.closed")
    @SchedulerLock(name = "sequential-inbox-housekeeping-closed", lockAtLeastFor = "5s", lockAtMostFor = "1h")
    public void deleteClosedSequenceInstances() {
        log.debug("Starting housekeeping task: deleting closed sequence instances and related data");

        // Delete in order to respect foreign key constraints
        int messagesDeleted = messageRepository.deleteMessagesForClosedSequences();
        int sequenceInstancesDeleted = sequenceInstanceRepository.deleteAllClosed();

        log.info("Sequential inbox housekeeping completed: deleted {} messages and {} closed sequence instances",
                messagesDeleted, sequenceInstancesDeleted);
    }
}
