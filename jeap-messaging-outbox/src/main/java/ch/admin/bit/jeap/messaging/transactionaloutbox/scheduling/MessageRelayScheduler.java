package ch.admin.bit.jeap.messaging.transactionaloutbox.scheduling;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.MessageRelay;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class MessageRelayScheduler {

    private final MessageRelay messageRelay;

    @SuppressWarnings("SpringElInspection")
    @Scheduled(fixedDelayString = "#{@txOutboxConfigProps.pollDelay}")
    @SchedulerLock(name = "outbox-message-relay-tasks",
            // If more than one outbox instances are running in parallel all try to relay messages by polling with a fixed delay.
            // By setting the minimum lock lifespan to pollDelay/2 the start of a new message relay task cannot be sooner
            // than pollDelay/2 after the previous task started.
            lockAtLeastFor = "#{@txOutboxConfigProps.pollDelay.dividedBy(2L)}",
            // A message relay task execution is expected to not take longer than continuousRelayTimeout + (maxDurationSendScheduled * messageRelayBatchSize)
            // We're adding a safety factor of 1.5 to the second part. The maximum lock lifespan should be kept as small as possible because this is also the duration it would
            // take for a different outbox instance to take over from a failed instance that did not release the lock. We would want this to happen as fast as possible.
            lockAtMostFor = "#{@txOutboxConfigProps.continuousRelayTimeout.plus(" +
                            "@txOutboxConfigProps.maxDurationSendScheduled.multipliedBy(@txOutboxConfigProps.messageRelayBatchSize * 3L).dividedBy(2L))}")
    void scheduleOutboxMessageRelay() {
        LockAssert.assertLocked();
        messageRelay.relay();
    }

}
