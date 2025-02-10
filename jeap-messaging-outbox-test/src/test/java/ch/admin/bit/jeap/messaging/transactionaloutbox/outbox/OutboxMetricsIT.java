package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestEvent;
import io.restassured.RestAssured;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("SameParameterValue")
@DirtiesContext
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.endpoint.prometheus.enabled=true",
        "management.endpoints.web.exposure.include=*",
        "jeap.messaging.transactional-outbox.poll-delay=1s"})
class OutboxMetricsIT extends KafkaIntegrationTestBase {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private TransactionalOutbox transactionalOutbox;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    OutboxMetrics outboxMetrics;

    @SuppressWarnings("unused")
    @MockitoBean
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    DeferredMessageRepository deferredMessageRepository;

    @Autowired
    DeferredMessageTestUtil deferredMessageTestUtil;

    @LocalServerPort
    int localServerPort;

    @AfterEach
    void cleanUp() {
        deferredMessageTestUtil.deleteAllMessages();
    }

    @SneakyThrows
    @Transactional
    @Commit
    @Test
    void testOutboxMetrics() {

        assertThat(deferredMessageRepository.findAll()).isEmpty();

        final String metricsBefore = RestAssured.given().port(localServerPort).get("/actuator/prometheus").getBody().asString();
        assertReadyToBeSentCount(metricsBefore, 0);
        assertFailedCountResendDisabled(metricsBefore, 0);
        assertFailedCountResendEnabled(metricsBefore, 0);
        assertPostTotalImmediateCommitted(metricsBefore, 0);
        assertPostTotalImmediateRolledBack(metricsBefore, 0);
        assertPostTotalImmediateUnknown(metricsBefore, 0);
        assertPostTotalScheduledCommitted(metricsBefore, 0);
        assertPostTotalScheduledRolledBack(metricsBefore, 0);
        assertPostTotalScheduledUnknown(metricsBefore, 0);

        final TestEvent testEvent1 = TestEventBuilder.create().idempotenceId("idempotenceId1").build();
        final TestEvent testEvent2 = TestEventBuilder.create().idempotenceId("idempotenceId2").build();
        final TestEvent testEvent3 = TestEventBuilder.create().idempotenceId("idempotenceId3").build();
        transactionalOutbox.sendMessage(testEvent1, TestEventConsumer.TOPIC);
        transactionalOutbox.sendMessageScheduled(testEvent2, TestEventConsumer.TOPIC);
        transactionalOutbox.sendMessage(testEvent3, TestEventConsumer.TOPIC);
        TestTransaction.end();
        TestTransaction.start();
        final TestEvent testEvent4 = TestEventBuilder.create().idempotenceId("idempotenceId4").build();
        final TestEvent testEvent5 = TestEventBuilder.create().idempotenceId("idempotenceId5").build();
        final TestEvent testEvent6 = TestEventBuilder.create().idempotenceId("idempotenceId6").build();
        transactionalOutbox.sendMessage(testEvent4, TestEventConsumer.TOPIC);
        transactionalOutbox.sendMessageScheduled(testEvent5, TestEventConsumer.TOPIC);
        transactionalOutbox.sendMessageScheduled(testEvent6, TestEventConsumer.TOPIC);
        TestTransaction.flagForRollback();
        TestTransaction.end();
        deferredMessageTestUtil.waitTillAllMessagesProcessed();
        outboxMetrics.updateGauges();

        final String metricsAfter = RestAssured.given().port(localServerPort).get("/actuator/prometheus").getBody().asString();
        assertReadyToBeSentCount(metricsAfter, 0);
        assertFailedCountResendEnabled(metricsAfter, 0);
        assertFailedCountResendDisabled(metricsAfter, 0);
        assertPostTotalImmediateCommitted(metricsAfter, 2);
        assertPostTotalImmediateRolledBack(metricsAfter, 1);
        assertPostTotalImmediateUnknown(metricsAfter, 0);
        assertPostTotalScheduledCommitted(metricsAfter, 1);
        assertPostTotalScheduledRolledBack(metricsAfter, 2);
        assertPostTotalScheduledUnknown(metricsAfter, 0);
        assertTransmitSecondsCountImmediate(metricsAfter, 2);
        assertTransmitSecondsCountScheduled(metricsAfter, 1);
    }

    private void assertReadyToBeSentCount(String metrics, int count) {
        assertThat(metrics).contains(String.format("outbox_messages_ready_to_be_sent_count %d.0", count));
    }

    private void assertFailedCountResendEnabled(String metrics, int count) {
        assertThat(metrics).contains(String.format("outbox_messages_failed_count{resend_status=\"resend_enabled\"} %d.0", count));
    }

    private void assertFailedCountResendDisabled(String metrics, int count) {
        assertThat(metrics).contains(String.format("outbox_messages_failed_count{resend_status=\"resend_disabled\"} %d.0", count));
    }


    private void assertPostTotalImmediateCommitted(String metrics, int count) {
        assertThat(metrics).contains(String.format("outbox_messages_post_total{delivery_type=\"immediate\",tx_status=\"committed\"} %d.0", count));
    }

    private void assertPostTotalScheduledCommitted(String metrics, int count) {
        assertThat(metrics).contains(String.format("outbox_messages_post_total{delivery_type=\"scheduled\",tx_status=\"committed\"} %d.0", count));
    }

    private void assertPostTotalImmediateRolledBack(String metrics, int count) {
        assertThat(metrics).contains(String.format("outbox_messages_post_total{delivery_type=\"immediate\",tx_status=\"rolled_back\"} %d.0", count));
    }

    private void assertPostTotalScheduledRolledBack(String metrics, int count) {
        assertThat(metrics).contains(String.format("outbox_messages_post_total{delivery_type=\"scheduled\",tx_status=\"rolled_back\"} %d.0", count));
    }

    private void assertPostTotalImmediateUnknown(String metrics, int count) {
        assertThat(metrics).contains(String.format("outbox_messages_post_total{delivery_type=\"immediate\",tx_status=\"unknown\"} %d.0", count));
    }

    private void assertPostTotalScheduledUnknown(String metrics, int count) {
        assertThat(metrics).contains(String.format("outbox_messages_post_total{delivery_type=\"scheduled\",tx_status=\"unknown\"} %d.0", count));
    }

    private void assertTransmitSecondsCountImmediate(String metrics, int count) {
        assertThat(metrics).contains(String.format("outbox_messages_transmit_seconds_count{class=\"ch.admin.bit.jeap.messaging.transactionaloutbox.messaging.KafkaDeferredMessageSender\"," +
                "delivery_type=\"immediate\",exception=\"none\",method=\"sendAsImmediate\"} %d", count));
    }

    private void assertTransmitSecondsCountScheduled(String metrics, int count) {
        assertThat(metrics).contains(String.format("outbox_messages_transmit_seconds_count{class=\"ch.admin.bit.jeap.messaging.transactionaloutbox.messaging.KafkaDeferredMessageSender\"," +
                "delivery_type=\"scheduled\",exception=\"none\",method=\"sendAsScheduled\"} %d", count));
    }

}
