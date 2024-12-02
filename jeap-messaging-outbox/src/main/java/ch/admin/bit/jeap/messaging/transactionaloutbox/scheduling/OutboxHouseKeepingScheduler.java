package ch.admin.bit.jeap.messaging.transactionaloutbox.scheduling;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxHouseKeeping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxHouseKeepingScheduler {

    private final OutboxHouseKeeping outboxHouseKeeping;

    @Scheduled(cron = "#{@txOutboxConfigProps.houseKeepingSchedule}")
    @SchedulerLock(name = "outbox-message-house-keeping-tasks", lockAtLeastFor = "5s", lockAtMostFor = "2h")
    void scheduleHouseKeeping() {
        LockAssert.assertLocked();
        outboxHouseKeeping.deleteOldMessages();
    }

}
