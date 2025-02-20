package ch.admin.bit.jeap.messaging.transactionaloutbox.metrics;

import ch.admin.bit.jeap.messaging.kafka.metrics.KafkaMessagingMetrics;
import ch.admin.bit.jeap.messaging.kafka.signature.publisher.SignaturePublisherProperties;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessageRepository;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.FailedMessageRepository;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

@RequiredArgsConstructor
public class MicrometerOutboxMetrics implements OutboxMetrics {

    private final MeterRegistry meterRegistry;
    private final DeferredMessageRepository deferredMessageRepository;
    private final FailedMessageRepository failedMessageRepository;
    private final KafkaMessagingMetrics kafkaMessagingMetrics;
    private final SignaturePublisherProperties signaturePublisherProperties;
    private final String applicationName;

    private int messagesReadyToBeSentCount = -1;
    private int messagesFailedWithoutResendCount = -1;
    private int messagesFailedWithResendCount = -1;
    private Counter messagesPostImmediateDeliveryCommittedCounter;
    private Counter messagesPostImmediateDeliveryRolledBackCounter;
    private Counter messagesPostImmediateDeliveryUnknownTxStateCounter;
    private Counter messagesPostScheduledDeliveryCommittedCounter;
    private Counter messagesPostScheduledDeliveryRolledBackCounter;
    private Counter messagesPostScheduledDeliveryUnknownTxStateCounter;

    @PostConstruct
    void initialize() {
        updateGauges();
        Gauge.builder(MESSAGES_READY_TO_BE_SENT_COUNTER, () -> messagesReadyToBeSentCount)
                .description("Outbox messages ready to be sent (aka 'the lag').")
                .register(meterRegistry);
        Gauge.builder(MESSAGES_FAILED_COUNTER, () -> messagesFailedWithoutResendCount)
                .tag(MESSAGE_RESEND_STATUS_TAG, MESSAGE_RESEND_STATUS_RESEND_DISABLED)
                .description("Outbox messages in state 'failed' with resend disabled.")
                .register(meterRegistry);
        Gauge.builder(MESSAGES_FAILED_COUNTER, () -> messagesFailedWithResendCount)
                .tag(MESSAGE_RESEND_STATUS_TAG, MESSAGE_RESEND_STATUS_RESEND_ENABLED)
                .description("Outbox messages in state 'failed' with resend enabled.")
                .register(meterRegistry);
        messagesPostImmediateDeliveryCommittedCounter = Counter.builder(MESSAGES_POST_COUNTER)
                .tag(MESSAGE_DELIVERY_TYPE_TAG, MESSAGE_DELIVERY_TYPE_IMMEDIATE)
                .tag(MESSAGE_TX_STATUS_TAG, MESSAGE_TX_STATUS_COMMITTED)
                .description("Message posts to the outbox for immediate delivery that were committed.")
                .register(meterRegistry);
        messagesPostImmediateDeliveryRolledBackCounter = Counter.builder(MESSAGES_POST_COUNTER)
                .tag(MESSAGE_DELIVERY_TYPE_TAG, MESSAGE_DELIVERY_TYPE_IMMEDIATE)
                .tag(MESSAGE_TX_STATUS_TAG, MESSAGE_TX_STATUS_ROLLED_BACK)
                .description("Message posts to the outbox for immediate delivery that were rolled back.")
                .register(meterRegistry);
        messagesPostImmediateDeliveryUnknownTxStateCounter = Counter.builder(MESSAGES_POST_COUNTER)
                .tag(MESSAGE_DELIVERY_TYPE_TAG, MESSAGE_DELIVERY_TYPE_IMMEDIATE)
                .tag(MESSAGE_TX_STATUS_TAG, MESSAGE_TX_STATUS_UNKNOWN)
                .description("Message posts to the outbox for immediate delivery where the final transaction state is unknown.")
                .register(meterRegistry);
        messagesPostScheduledDeliveryCommittedCounter = Counter.builder(MESSAGES_POST_COUNTER)
                .tag(MESSAGE_DELIVERY_TYPE_TAG, MESSAGE_DELIVERY_TYPE_SCHEDULED)
                .tag(MESSAGE_TX_STATUS_TAG, MESSAGE_TX_STATUS_COMMITTED)
                .description("Message posts to the outbox for scheduled delivery that were committed.")
                .register(meterRegistry);
        messagesPostScheduledDeliveryRolledBackCounter = Counter.builder(MESSAGES_POST_COUNTER)
                .tag(MESSAGE_DELIVERY_TYPE_TAG, MESSAGE_DELIVERY_TYPE_SCHEDULED)
                .tag(MESSAGE_TX_STATUS_TAG, MESSAGE_TX_STATUS_ROLLED_BACK)
                .description("Message posts to the outbox for scheduled delivery that were rolled back.")
                .register(meterRegistry);
        messagesPostScheduledDeliveryUnknownTxStateCounter = Counter.builder(MESSAGES_POST_COUNTER)
                .tag(MESSAGE_DELIVERY_TYPE_TAG, MESSAGE_DELIVERY_TYPE_SCHEDULED)
                .tag(MESSAGE_TX_STATUS_TAG, MESSAGE_TX_STATUS_UNKNOWN)
                .description("Message posts to the outbox for scheduled delivery where the final transaction state is unknown.")
                .register(meterRegistry);
    }


    @Override
    public void countTransactionalSend(boolean sendImmediately) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            getTransactionalSendCountTxSync().incrementSendCount(sendImmediately);
        }
        else {
            countSendWithoutTxSynchronization(sendImmediately);
        }
    }

    private void countSendWithoutTxSynchronization(boolean sendImmediately) {
        if (sendImmediately) {
            incrementMessagesPostImmediateDeliveryUnknownTxStateCounter(1);
        }
        else {
            incrementMessagesPostScheduledDeliveryUnknownTxStateCounter(1);
        }
    }

    @Override
    public void updateGauges() {
        messagesReadyToBeSentCount = deferredMessageRepository.countMessagesReadyToBeSent();
        messagesFailedWithoutResendCount = failedMessageRepository.countFailedMessages(false);
        messagesFailedWithResendCount = failedMessageRepository.countFailedMessages(true);
    }

    @Override
    public void countMessagingSend(String bootstrapServers, String topic, String messageType, String messageTypeVersion) {
        kafkaMessagingMetrics.incrementSend(bootstrapServers, applicationName, topic, messageType, messageTypeVersion, signaturePublisherProperties.isSigningEnabled());
    }

    void incrementMessagesPostImmediateDeliveryCommittedCount(int amount) {
        messagesPostImmediateDeliveryCommittedCounter.increment(amount);
    }

    void incrementMessagesPostImmediateDeliveryRolledBackCount(int amount) {
        messagesPostImmediateDeliveryRolledBackCounter.increment(amount);
    }

    void incrementMessagesPostImmediateDeliveryUnknownTxStateCounter(int amount) {
        messagesPostImmediateDeliveryUnknownTxStateCounter.increment(amount);
    }

    void incrementMessagesPostScheduledDeliveryCommittedCount(int amount) {
        messagesPostScheduledDeliveryCommittedCounter.increment(amount);
    }

    void incrementMessagesPostScheduledDeliveryRolledBackCount(int amount) {
        messagesPostScheduledDeliveryRolledBackCounter.increment(amount);
    }

    void incrementMessagesPostScheduledDeliveryUnknownTxStateCounter(int amount) {
        messagesPostScheduledDeliveryUnknownTxStateCounter.increment(amount);
    }

    private TransactionalSendCountTxSync getTransactionalSendCountTxSync() {
        return getRegisteredTransactionalSendCountTxSync()
                .orElseGet(this::registerNewTransactionalSendCountTxSync);
    }

    private Optional<TransactionalSendCountTxSync> getRegisteredTransactionalSendCountTxSync() {
        return TransactionSynchronizationManager.getSynchronizations().stream()
                .filter(txSync -> txSync instanceof TransactionalSendCountTxSync)
                .map(txSync -> (TransactionalSendCountTxSync) txSync)
                .findFirst();
    }

    private TransactionalSendCountTxSync registerNewTransactionalSendCountTxSync() {
        var transactionalSendCountTxSync = new TransactionalSendCountTxSync(this);
        TransactionSynchronizationManager.registerSynchronization(transactionalSendCountTxSync);
        return transactionalSendCountTxSync;
    }


    private static class TransactionalSendCountTxSync implements TransactionSynchronization {

        private final MicrometerOutboxMetrics micrometerOutboxMetrics;
        private int sendImmediatelyCount;
        private int sendScheduledCount;

        private TransactionalSendCountTxSync(MicrometerOutboxMetrics micrometerOutboxMetrics) {
            this.micrometerOutboxMetrics = micrometerOutboxMetrics;
            resetCounters();
        }

        private void incrementSendCount(boolean sendImmediately) {
            if (sendImmediately) {
                sendImmediatelyCount++;
            } else {
                sendScheduledCount++;
            }
        }

        @Override
        public void afterCompletion(int status) {
            switch (status) {
                case TransactionSynchronization.STATUS_COMMITTED:
                    micrometerOutboxMetrics.incrementMessagesPostImmediateDeliveryCommittedCount(sendImmediatelyCount);
                    micrometerOutboxMetrics.incrementMessagesPostScheduledDeliveryCommittedCount(sendScheduledCount);
                    break;
                case TransactionSynchronization.STATUS_ROLLED_BACK:
                    micrometerOutboxMetrics.incrementMessagesPostImmediateDeliveryRolledBackCount(sendImmediatelyCount);
                    micrometerOutboxMetrics.incrementMessagesPostScheduledDeliveryRolledBackCount(sendScheduledCount);
                    break;
                default:
                    micrometerOutboxMetrics.incrementMessagesPostImmediateDeliveryUnknownTxStateCounter(sendImmediatelyCount);
                    micrometerOutboxMetrics.incrementMessagesPostScheduledDeliveryUnknownTxStateCounter(sendScheduledCount);
            }
            resetCounters();
        }

        private void resetCounters() {
            sendImmediatelyCount = 0;
            sendScheduledCount = 0;
        }
    }

}
