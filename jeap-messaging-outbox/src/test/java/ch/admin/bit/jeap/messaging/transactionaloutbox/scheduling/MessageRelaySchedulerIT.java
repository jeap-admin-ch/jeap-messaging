package ch.admin.bit.jeap.messaging.transactionaloutbox.scheduling;

import ch.admin.bit.jeap.messaging.transactionaloutbox.config.TransactionalOutboxConfigurationProperties;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@EnableAutoConfiguration
@DataJpaTest
@ContextConfiguration(classes = {OutboxSchedulingConfig.class, TransactionalOutboxConfigurationProperties.class})
public class MessageRelaySchedulerIT {

    @MockBean
    MessageRelay messageRelayMock;

    @MockBean
    OutboxHouseKeeping outboxHouseKeepingMock;

    @MockBean
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

