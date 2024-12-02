package ch.admin.bit.jeap.messaging.transactionaloutbox.scheduling;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
public class OutboxMetricsUpdateScheduler {

    private final OutboxMetrics outboxMetrics;

    @Scheduled(fixedRateString = "#{@txOutboxConfigProps.metricsUpdateInterval}")
    void scheduleMetricsUpdate() {
        outboxMetrics.updateGauges();
    }

}
