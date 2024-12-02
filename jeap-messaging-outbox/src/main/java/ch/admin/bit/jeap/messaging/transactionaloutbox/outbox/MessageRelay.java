package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.transactionaloutbox.spring.DeferredMessageSenderProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRelay {

    private final DeferredMessageSenderProvider deferredMessageSenderProvider;
    private final DeferredMessageSendExceptionHandler exceptionHandler;
    private final DeferredMessageRepository deferredMessageRepository;
    private final TransactionalOutboxConfiguration config;

    public void relay() {
        log.debug("Starting to relay deferred messages.");
        final ZonedDateTime stopRelayingAt = ZonedDateTime.now().plus(config.getContinuousRelayTimeout());
        log.debug("Will stop relaying after {}.", stopRelayingAt);
        while (ZonedDateTime.now().isBefore(stopRelayingAt)) {

            log.debug("Fetching at most {} deferred messages ready to be sent.", config.getMessageRelayBatchSize());
            List<DeferredMessage> messages = deferredMessageRepository.findMessagesReadyToBeSent(config.getMessageRelayBatchSize());
            if (messages.isEmpty()) {
                log.debug("There are no deferred messages ready to be sent.");
                break;
            }

            log.debug("Fetched a batch of {} deferred messages to send.", messages.size());
            try {
                sendMessages(messages);
            } catch (Exception e) {
                log.error("Unable to send the complete batch of fetched deferred messages.", e);
                break;
            }

        }
        log.debug("Ending relaying of deferred messages.");
    }

    private void sendMessages(List<DeferredMessage> messages) {

        log.debug("Starting to send {} deferred messages.", messages.size());
        for (DeferredMessage message : messages) {
            sendMessage(message);
        }
        log.debug("Ending sending deferred messages.");
    }

    private void sendMessage(DeferredMessage message) {
        try {
            DeferredMessageSender deferredMessageSender = deferredMessageSenderProvider.getDeferredMessageSenderForCluster(message);
            deferredMessageSender.sendAsScheduled(message);
            deferredMessageRepository.markSentScheduled(message.getId(), ZonedDateTime.now());
        } catch (DeferredMessageSendException e) {
            exceptionHandler.handle(message, e);
        }
    }
}
