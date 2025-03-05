package ch.admin.bit.jeap.messaging.sequentialinbox.metrics;

import java.time.Duration;

public interface SequentialInboxMetricsCollector {

    void onConsumedSequencedMessage(String messageType);

    void onWaitingMessageCompleted(String messageType, Duration waitDuration);
}
