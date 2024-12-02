package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.transaction.TestTransaction;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@Component
@RequiredArgsConstructor
class DeferredMessageTestUtil {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final DeferredMessageRepository deferredMessageRepository;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final JdbcTemplate jdbcTemplate;

    void waitTillAllMessagesProcessed() {
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            TestTransaction.start();
            boolean unprocessedMessageExists = deferredMessageRepository.findAll().stream().anyMatch(
                    message -> (message.getFailed() == null) && (message.getSentScheduled() == null) && (message.getSentImmediately() == null));
            TestTransaction.end();
            return !unprocessedMessageExists;
        });
    }

    void deleteAllMessages() {
        if (TestTransaction.isActive()) {
            TestTransaction.end();
        }
        TestTransaction.start();
        deferredMessageRepository.findAll().forEach(m -> deferredMessageRepository.deleteById(m.getId()));
        TestTransaction.end();
    }

    @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection", "SameParameterValue"})
    void alterClusterNameAndMakeAvailableForMessageRelay(String idempotenceId, String newClusterName) {
        DeferredMessage deferredMessage = deferredMessageRepository.findAll().stream().
                filter(m -> idempotenceId.equals(m.getMessageIdempotenceId())).
                findFirst().
                orElseThrow(() -> new IllegalStateException(
                        "No persisted deferred message found with idempotence id " + idempotenceId));
        String updateMessageQuery = """
                    UPDATE deferred_message m
                    SET m.cluster_name = ?, m.sent_scheduled = null, m.sent_immediately = null
                    where m.id = ?""";
        jdbcTemplate.update(updateMessageQuery, newClusterName, deferredMessage.getId());
    }

}
