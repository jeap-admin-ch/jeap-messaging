package ch.admin.bit.jeap.messaging.sequentialinbox.metrics;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequencedMessageType;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequentialInboxConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Provides metrics for the SequentialInbox:
 * <ul>
 * <li>Number of messages per SequencedMessageType in state WAITING</li>
 * <li>Average waiting time per SequencedMessageType (only waiting messages are taken into account)</li>
 * <li>Total number of messages consumed by the inbox</li>
 * </ul>
 */
@RequiredArgsConstructor
class SequentialInboxMetrics implements SequentialInboxMetricsCollector {

    private static final String WAITING_MESSAGES = "jeap.messaging.sequential-inbox.waiting-messages";
    private static final String CONSUMED_MESSAGES = "jeap.messaging.sequential-inbox.consumed-messages";
    private static final String WAITING_MESSAGE_DELAY = "jeap.messaging.sequential-inbox.waiting-message-delay";
    private static final String TYPE_TAG = "type";

    private final MeterRegistry meterRegistry;
    private final SequentialInboxConfiguration config;
    private final SequentialInboxPersistenceMetrics persistenceMetrics;

    @PostConstruct
    public void initMetrics() {
        persistenceMetrics.updateMetrics();
        sequencedMessageTypes().forEach(this::registerTypeMetrics);
    }

    private void registerTypeMetrics(String type) {
        meterRegistry.gauge(WAITING_MESSAGES, Tags.of(TYPE_TAG, type), type, persistenceMetrics::getWaitingMessagesOfType);
    }

    @Scheduled(fixedRateString = "${jeap.messaging.sequential-inbox.metrics.update-rate-minutes:5}", timeUnit = MINUTES)
    public void onScheduleMetricsUpdate() {
        persistenceMetrics.updateMetrics();
    }

    @Override
    public void onWaitingMessageCompleted(String messageType, Duration waitDuration) {
        meterRegistry.timer(WAITING_MESSAGE_DELAY, TYPE_TAG, messageType)
                .record(waitDuration);
    }

    @Override
    public void onConsumedSequencedMessage(String messageType) {
        meterRegistry.counter(CONSUMED_MESSAGES, TYPE_TAG, messageType)
                .increment();
    }

    private Stream<String> sequencedMessageTypes() {
        return config.getSequencedMessageTypes().stream()
                .map(SequencedMessageType::getQualifiedName);
    }
}
