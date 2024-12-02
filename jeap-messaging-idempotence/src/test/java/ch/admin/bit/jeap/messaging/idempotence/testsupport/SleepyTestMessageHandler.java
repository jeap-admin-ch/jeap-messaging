package ch.admin.bit.jeap.messaging.idempotence.testsupport;

import ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandler;
import ch.admin.bit.jeap.messaging.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public class SleepyTestMessageHandler {

    private final Duration delay;

    @SneakyThrows
    @IdempotentMessageHandler
    public void handleMessage(Message message) {
        log.info("Handling message with idempotence id '{}'. This will take {} millis.",
                message.getIdentity().getIdempotenceId(), delay.toMillis());
        Thread.sleep(delay.toMillis());
        log.info("Finished handling message with idempotence id '{}' after {} millis.",
                message.getIdentity().getIdempotenceId(), delay.toMillis());
    }

}
