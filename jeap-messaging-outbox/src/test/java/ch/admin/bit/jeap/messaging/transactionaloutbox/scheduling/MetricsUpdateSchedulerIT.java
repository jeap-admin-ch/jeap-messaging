package ch.admin.bit.jeap.messaging.transactionaloutbox.scheduling;

import ch.admin.bit.jeap.messaging.kafka.metrics.KafkaMessagingMetrics;
import ch.admin.bit.jeap.messaging.transactionaloutbox.config.TransactionalOutboxConfigurationProperties;
import ch.admin.bit.jeap.messaging.transactionaloutbox.metrics.OutboxMetricsConfig;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessageRepository;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.FailedMessageRepository;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.MessageRelay;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxHouseKeeping;
import io.micrometer.core.instrument.MeterRegistry;
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
@ContextConfiguration(classes = {OutboxSchedulingConfig.class, OutboxMetricsConfig.class, TransactionalOutboxConfigurationProperties.class})
public class MetricsUpdateSchedulerIT {

    @MockBean
    DeferredMessageRepository deferredMessageRepository;

    @MockBean
    FailedMessageRepository failedMessageRepository;

    @MockBean
    MeterRegistry meterRegistry;

    @MockBean
    MessageRelay messageRelay;

    @MockBean
    OutboxHouseKeeping outboxHouseKeeping;

    @MockBean
    KafkaMessagingMetrics kafkaMessagingMetrics;

    @SneakyThrows
    @Test
    void testFetchGaugesValuesFromRepository() {
        Thread.sleep(2000);
        // First fetch occurs on initialization of the MicrometerOutboxMetrics, then at least two additional fetches should
        // happen as we fetch every second and wait for two seconds. It may take some time for the test method to be executed
        // and start the two seconds wait, therefore an additional fetch might happen.
        Mockito.verify(deferredMessageRepository, Mockito.atLeast(3)).countMessagesReadyToBeSent();
        Mockito.verify(deferredMessageRepository, Mockito.atMost(4)).countMessagesReadyToBeSent();
        Mockito.verify(failedMessageRepository, Mockito.atLeast(3)).countFailedMessages(false);
        Mockito.verify(failedMessageRepository, Mockito.atMost(4)).countFailedMessages(false);
        Mockito.verify(failedMessageRepository, Mockito.atLeast(3)).countFailedMessages(true);
        Mockito.verify(failedMessageRepository, Mockito.atMost(4)).countFailedMessages(true);

        Mockito.verifyNoMoreInteractions(deferredMessageRepository);
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("jeap.messaging.transactional-outbox.metrics-update-interval", () -> "1s");
    }

}

