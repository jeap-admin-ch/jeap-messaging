package ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing;


import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest( classes = {IdempotentProcessingConfig.class},
                 properties = {"jeap.messaging.idempotent-processing.idempotent-processing-retention-duration=P14D"})
public class IdempotentProcessingHouseKeepingTest {

    @Autowired
    private IdempotentProcessingHouseKeeping idempotentProcessingHouseKeeping;

    @Autowired
    private IdempotentProcessingConfig config;

    @MockBean
    private IdempotentProcessingRepository repository;

    @Captor
    private ArgumentCaptor<ZonedDateTime> zonedDateTimeCaptor;

    @Test
    void testDeleteOldIdempotentProcessingRecords() {
        when(repository.deleteAllCreatedBefore(any(ZonedDateTime.class))).thenReturn(1);
        final ZonedDateTime beforeHouseKeeping = ZonedDateTime.now();

        idempotentProcessingHouseKeeping.deleteOldIdempotentProcessingRecords();

        verify(repository, Mockito.times(1)).deleteAllCreatedBefore(zonedDateTimeCaptor.capture());
        verifyNoMoreInteractions(repository);
        final ZonedDateTime approximateCutOffTimePoint =  beforeHouseKeeping.minus(config.getIdempotentProcessingRetentionDuration());
        assertThat(zonedDateTimeCaptor.getValue()).isBetween(approximateCutOffTimePoint, approximateCutOffTimePoint.plus(500, ChronoUnit.MILLIS));
    }

}
