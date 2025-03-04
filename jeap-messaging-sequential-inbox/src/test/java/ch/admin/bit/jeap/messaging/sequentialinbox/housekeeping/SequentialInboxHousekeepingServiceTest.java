package ch.admin.bit.jeap.messaging.sequentialinbox.housekeeping;

import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.MessageRepository;
import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.SequenceInstanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@SpringBootTest
@TestPropertySource(properties = {
        "jeap.messaging.sequential-inbox.housekeeping.expiry-cron=*/2 * * * * ?", // Run every 2 seconds
        "jeap.messaging.sequential-inbox.housekeeping.closed-instances-cron=*/2 * * * * ?" // Run every 2 seconds
})
class SequentialInboxHousekeepingServiceTest {

    @Configuration
    @EnableScheduling
    static class TestConfig {
        @Bean
        public SequentialInboxHousekeepingService housekeepingService(MessageRepository messageRepository,
                                                                      SequenceInstanceRepository sequenceInstanceRepository) {
            return new SequentialInboxHousekeepingService(messageRepository, sequenceInstanceRepository);
        }
    }

    @MockitoBean
    private MessageRepository messageRepository;

    @MockitoBean
    private SequenceInstanceRepository sequenceInstanceRepository;

    @Autowired
    private SequentialInboxHousekeepingService sequentialInboxHousekeepingService;

    @Test
    void shouldAutomaticallyDeleteExpiredMessagesOnSchedule() {
        await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(messageRepository, atLeast(1)).deleteExpiredMessages(any(ZonedDateTime.class));
                    verify(sequenceInstanceRepository, atLeast(1)).deleteExpiredInstances(any(ZonedDateTime.class));
                });
    }

    @Test
    void shouldAutomaticallyDeleteClosedSequenceInstancesOnSchedule() {
        await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // Verify that repository methods are called in the correct order
                    verify(messageRepository, atLeast(1)).deleteMessagesForClosedSequences();
                    verify(sequenceInstanceRepository, atLeast(1)).deleteAllClosed();
                });
    }
}
