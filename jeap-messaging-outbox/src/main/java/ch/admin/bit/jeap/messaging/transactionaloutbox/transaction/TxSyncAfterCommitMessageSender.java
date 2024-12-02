package ch.admin.bit.jeap.messaging.transactionaloutbox.transaction;

import ch.admin.bit.jeap.messaging.transactionaloutbox.config.TransactionalOutboxConfigurationProperties;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.*;
import ch.admin.bit.jeap.messaging.transactionaloutbox.spring.DeferredMessageSenderProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArgument;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@RequiredArgsConstructor
@Component
public class TxSyncAfterCommitMessageSender implements AfterCommitMessageSender {

    private final DeferredMessageSenderProvider deferredMessageSenderProvider;
    private final DeferredMessageSendExceptionHandler exceptionHandler;
    private final DeferredMessageRepository deferredMessageRepository;
    private final PlatformTransactionManager transactionManager;
    private final TransactionalOutboxConfigurationProperties config;

    @Override
    public void sendImmediatelyAfterTransactionCommit(DeferredMessage deferredMessage) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("Transaction synchronization not available. Skipping immediate send after transaction commit for deferred message ({}).", messageIdLogArgument(deferredMessage));
            return;
        }
        try {
            getDeferredMessagesSendingTxSync().addDeferredMessage(deferredMessage);
            log.debug("Registered deferred message ({}) to be sent immediately.", messageIdLogArgument(deferredMessage));
        } catch (Exception e) {
            log.warn("Registering deferred message ({}) for after commit send failed. Skipping immediate send.", messageIdLogArgument(deferredMessage), e);
        }
    }

    private StructuredArgument messageIdLogArgument(DeferredMessage deferredMessage) {
        return kv("deferredMessageId", deferredMessage.getId());
    }

    private DeferredMessagesSendingTxSync getDeferredMessagesSendingTxSync() {
        return getRegisteredDeferredMessagesSendingTxSync()
                .orElseGet(this::registerNewDeferredMessagesSendingTxSync);
    }

    private Optional<DeferredMessagesSendingTxSync> getRegisteredDeferredMessagesSendingTxSync() {
        return TransactionSynchronizationManager.getSynchronizations().stream()
                .filter(txSync -> txSync instanceof DeferredMessagesSendingTxSync)
                .map(txSync -> (DeferredMessagesSendingTxSync) txSync)
                .findFirst();
    }

    private DeferredMessagesSendingTxSync registerNewDeferredMessagesSendingTxSync() {
        var deferredMessagesSendingTxSync = new DeferredMessagesSendingTxSync(deferredMessageSenderProvider,
                config.getMaxDurationSendImmediately(), exceptionHandler, deferredMessageRepository, transactionManager);
        TransactionSynchronizationManager.registerSynchronization(deferredMessagesSendingTxSync);
        return deferredMessagesSendingTxSync;
    }

    @Slf4j
    private static class DeferredMessagesSendingTxSync implements TransactionSynchronization {

        private final DeferredMessageSenderProvider deferredMessageSenderProvider;
        private final Duration maxSendDuration;
        private final DeferredMessageSendExceptionHandler exceptionHandler;
        private final DeferredMessageRepository deferredMessageRepository;
        private final TransactionTemplate transactionTemplate;


        private DeferredMessagesSendingTxSync(DeferredMessageSenderProvider deferredMessageSenderProvider, Duration maxSendDuration, DeferredMessageSendExceptionHandler exceptionHandler,
                                              DeferredMessageRepository deferredMessageRepository, PlatformTransactionManager transactionManager) {
            this.deferredMessageSenderProvider = deferredMessageSenderProvider;
            this.maxSendDuration = maxSendDuration;
            this.exceptionHandler = exceptionHandler;
            this.deferredMessageRepository = deferredMessageRepository;
            this.transactionTemplate = new TransactionTemplate(transactionManager);
            this.transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        }

        private List<DeferredMessage> deferredMessages = new ArrayList<>();

        private void addDeferredMessage(DeferredMessage deferredMessage) {
            deferredMessages.add(deferredMessage);
        }

        @Override
        public void beforeCommit(boolean readOnly) {
            final Duration relayDelay = maxSendDuration.multipliedBy(deferredMessages.size());
            final ZonedDateTime beforeCommitTime = ZonedDateTime.now();
            final ZonedDateTime scheduleAfter = beforeCommitTime.plus(relayDelay);
            deferredMessages.forEach(deferredMessage ->
                    deferredMessageRepository.setScheduleAfter(deferredMessage.getId(), scheduleAfter));
        }

        @Override
        public void afterCommit() {
            executeInNewTransaction(this::sendMessages);
        }

        private void sendMessages() {
            int numSentMessages = 0;
            try {
                for (DeferredMessage deferredMessage : deferredMessages) {
                    try {
                        DeferredMessageSender deferredMessageSender = deferredMessageSenderProvider.getDeferredMessageSenderForCluster(deferredMessage);
                        deferredMessageSender.sendAsImmediate(deferredMessage);
                        numSentMessages++;
                        deferredMessageRepository.markSentImmediately(deferredMessage.getId(), ZonedDateTime.now());
                    } catch (DeferredMessageSendException e) {
                        executeInNewTransaction(() -> exceptionHandler.handle(deferredMessage, e));
                    }
                }
            } catch (Exception e) {
                // The outbox message relay process will try to continue sending later (for unsent, not failed messages).
                int numUnsetMessages = deferredMessages.size() - numSentMessages;
                log.warn("Unable to send all deferred messages immediately after transaction commit, {} messages not sent.", numUnsetMessages, e);
            }
        }

        @Override
        public void afterCompletion(int status) {
            deferredMessages.clear();
        }

        private void executeInNewTransaction(Runnable r) {
            transactionTemplate.executeWithoutResult(status -> r.run());
        }
    }

}
