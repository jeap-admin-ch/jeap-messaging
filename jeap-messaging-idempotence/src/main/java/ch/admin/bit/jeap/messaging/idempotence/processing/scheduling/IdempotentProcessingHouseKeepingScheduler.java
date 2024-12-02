package ch.admin.bit.jeap.messaging.idempotence.processing.scheduling;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingHouseKeeping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentProcessingHouseKeepingScheduler {

    private final IdempotentProcessingHouseKeeping idempotentProcessingHouseKeeping;

    @Scheduled(cron = "#{@idempotentProcessingConfigProps.houseKeepingSchedule}")
    @SchedulerLock(name = "idempotent-processing-house-keeping-tasks", lockAtLeastFor = "5s", lockAtMostFor = "2h")
    void scheduleHouseKeeping() {
        LockAssert.assertLocked();
        idempotentProcessingHouseKeeping.deleteOldIdempotentProcessingRecords();
    }

}
