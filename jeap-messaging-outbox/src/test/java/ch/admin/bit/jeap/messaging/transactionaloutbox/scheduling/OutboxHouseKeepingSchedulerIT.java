package ch.admin.bit.jeap.messaging.transactionaloutbox.scheduling;

import ch.admin.bit.jeap.messaging.transactionaloutbox.config.TransactionalOutboxConfigurationProperties;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.MessageRelay;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxHouseKeeping;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxMetrics;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@EnableAutoConfiguration
@DataJpaTest
@ContextConfiguration(classes = {OutboxSchedulingConfig.class, TransactionalOutboxConfigurationProperties.class})
public class OutboxHouseKeepingSchedulerIT {

    @MockBean
    OutboxHouseKeeping outboxHouseKeepingMock;

    @MockBean
    MessageRelay messageRelayMock;

    @MockBean
    OutboxMetrics outboxMetricsMock;

    @SneakyThrows
    @Test
    void testDeleteOldMessagesCalled() {
        verify(outboxHouseKeepingMock, timeout(20000).times(1)).deleteOldMessages();
        verifyNoMoreInteractions(outboxHouseKeepingMock);
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("jeap.messaging.transactional-outbox.house-keeping-schedule", OutboxHouseKeepingSchedulerIT::getScheduleInTwoSecondsCronExpression);
    }

    private static String getScheduleInTwoSecondsCronExpression() {
        LocalDateTime now = LocalDateTime.now().plusSeconds(2);
        return String.format("%s %s %s * * *", now.getSecond(), now.getMinute(), now.getHour());
    }

}