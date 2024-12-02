package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeferredMessageSendExceptionHandler {

    private final DeferredMessageRepository deferredMessageRepository;

    public void handle(DeferredMessage deferredMessage, DeferredMessageSendException e) {
        if (e.getReason().causedByMessage) {
            log.error("Deferred message has an error and cannot be sent. Marking {} as failed.", DeferredMessageLogArgument.from(deferredMessage), e);
            deferredMessageRepository.markFailed(deferredMessage.getId(), ZonedDateTime.now(), e.getReason());
        }
        else {
            throw e;
        }
    }

}
