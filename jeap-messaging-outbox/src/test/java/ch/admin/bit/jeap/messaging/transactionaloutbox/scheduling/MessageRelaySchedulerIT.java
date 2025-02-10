package ch.admin.bit.jeap.messaging.transactionaloutbox.scheduling;

import ch.admin.bit.jeap.messaging.transactionaloutbox.config.TransactionalOutboxConfigurationProperties;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.MessageRelay;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxHouseKeeping;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxMetrics;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@EnableAutoConfiguration
@DataJpaTest
@ContextConfiguration(classes = {OutboxSchedulingConfig.class, TransactionalOutboxConfigurationProperties.class})
public class MessageRelaySchedulerIT {

    @MockitoBean
    MessageRelay messageRelayMock;

    @MockitoBean
    OutboxHouseKeeping outboxHouseKeepingMock;

    @MockitoBean
    OutboxMetrics outboxMetricsMock;

    @SneakyThrows
    @Test
    void testRelayCalled() {
        Thread.sleep(3000);
        Mockito.verify(messageRelayMock, Mockito.atLeast(2)).relay();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("jeap.messaging.transactional-outbox.poll-delay", () -> "1s");
        registry.add("jeap.messaging.transactional-outbox.continuous-relay-timeout", () -> "10s");
    }

}

