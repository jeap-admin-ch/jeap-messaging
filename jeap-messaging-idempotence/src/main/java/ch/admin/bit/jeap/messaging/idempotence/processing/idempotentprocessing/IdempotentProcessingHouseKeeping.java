package ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentProcessingHouseKeeping {

    private final IdempotentProcessingRepository idempotentProcessingRepository;
    private final IdempotentProcessingConfig config;

    public void deleteOldIdempotentProcessingRecords() {
        log.info("House keeping: deleting old idempotent processing records..");
        final ZonedDateTime deleteBefore = ZonedDateTime.now().minus(config.getIdempotentProcessingRetentionDuration());
        int deleted = idempotentProcessingRepository.deleteAllCreatedBefore(deleteBefore);
        log.info("House keeping: ..done. Deleted {} old idempotent processing records.", deleted);
    }

}
