package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessage;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessageRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.test.context.transaction.TestTransaction;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DeferredMessageTestUtil {

    private final DeferredMessageRepository deferredMessageRepository;

    public static DeferredMessageTestUtil with(DeferredMessageRepository deferredMessageRepository) {
        return new DeferredMessageTestUtil(deferredMessageRepository);
    }

    public void waitForMessageSentOrFailed(long id) {
        await().atMost(5, TimeUnit.SECONDS).until( () -> {
            TestTransaction.start();
            DeferredMessage message = deferredMessageRepository.getById(id);
            boolean sentOrFailed = (message.getFailed() != null) || (message.getSentScheduled() != null) || (message.getSentImmediately() != null);
            TestTransaction.end();
            return sentOrFailed;
        });
    }

    public void deleteAllMessagesAfter(Runnable runnable) {
        try {
            runnable.run();
        }
        finally {
            if (!TestTransaction.isActive()) {
                TestTransaction.start();
            }
            deferredMessageRepository.findAll().forEach( m -> deferredMessageRepository.deleteById(m.getId()));
            TestTransaction.end();
        }
    }

}
