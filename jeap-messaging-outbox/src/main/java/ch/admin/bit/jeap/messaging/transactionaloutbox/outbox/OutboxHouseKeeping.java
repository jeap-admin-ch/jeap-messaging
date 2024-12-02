package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxHouseKeeping {

    private final DeferredMessageRepository deferredMessageRepository;
    private final TransactionalOutboxConfiguration config;

    public void deleteOldMessages() {
        log.info("House keeping: deleting old messages..");
        ZonedDateTime now = ZonedDateTime.now();
        int deletedSent = deferredMessageRepository.deleteMessagesSentBefore(now.minus(config.getSentMessageRetentionDuration()));
        int deletedUnsent = deferredMessageRepository.deleteUnsentMessagesCreatedBefore(now.minus(config.getUnsentMessageRetentionDuration()));
        log.info("House keeping: ..done. Deleted {} sent messages and {} not yet sent messages.", deletedSent, deletedUnsent);
    }

}
