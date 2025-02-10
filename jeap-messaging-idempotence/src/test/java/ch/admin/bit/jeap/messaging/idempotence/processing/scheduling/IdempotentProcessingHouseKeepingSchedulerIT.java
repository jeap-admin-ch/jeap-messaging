package ch.admin.bit.jeap.messaging.idempotence.processing.scheduling;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingConfig;
import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingHouseKeeping;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@EnableAutoConfiguration
@DataJpaTest
@ContextConfiguration(classes = {IdempotentProcessingSchedulingConfig.class, IdempotentProcessingConfig.class})
public class IdempotentProcessingHouseKeepingSchedulerIT {

    @MockitoBean
    IdempotentProcessingHouseKeeping idempotentProcessingHouseKeepingMock;

    @SneakyThrows
    @Test
    void testDeleteOldIdempotentProcessingRecordsCalled() {
        verify(idempotentProcessingHouseKeepingMock, timeout(10000).times(1)).deleteOldIdempotentProcessingRecords();
        verifyNoMoreInteractions(idempotentProcessingHouseKeepingMock);
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("jeap.messaging.idempotent-processing.house-keeping-schedule", IdempotentProcessingHouseKeepingSchedulerIT::getScheduleInTwoSecondsCronExpression);
    }

    private static String getScheduleInTwoSecondsCronExpression() {
        LocalDateTime now = LocalDateTime.now().plusSeconds(2);
        return String.format("%s %s %s * * *", now.getSecond(), now.getMinute(), now.getHour());
    }

}
