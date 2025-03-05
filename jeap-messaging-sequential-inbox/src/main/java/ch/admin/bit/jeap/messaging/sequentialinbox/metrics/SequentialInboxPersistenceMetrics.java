package ch.admin.bit.jeap.messaging.sequentialinbox.metrics;

import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Slf4j
class SequentialInboxPersistenceMetrics {

    private final MessageRepository messageRepository;

    private final Map<String, Double> waitingMessageCountByType = new ConcurrentHashMap<>();

    @Transactional
    public void updateMetrics() {
        log.debug("Updating scheduled inbox metrics");
        waitingMessageCountByType.putAll(messageRepository.getWaitingMessageCountByType());
    }

    public double getWaitingMessagesOfType(String type) {
        return waitingMessageCountByType.getOrDefault(type, 0.0);
    }
}
