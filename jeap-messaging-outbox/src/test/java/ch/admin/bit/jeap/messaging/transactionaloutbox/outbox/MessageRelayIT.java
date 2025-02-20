package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.metrics.KafkaMessagingMetrics;
import ch.admin.bit.jeap.messaging.kafka.signature.publisher.SignaturePublisherProperties;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport.DeferredMessageTestUtil;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport.OutboxMockKafkaTestConfig;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport.StringMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "jeap.messaging.transactional-outbox.poll-delay=1s",
        "jeap.messaging.transactional-outbox.message-relay-batch-size=2",
        "jeap.messaging.transactional-outbox.continuous-relay-timeout=3s"})
@ContextConfiguration(classes = OutboxMockKafkaTestConfig.class)
class MessageRelayIT {

    @Autowired
    TransactionalOutbox transactionalOutbox;

    @Autowired
    DeferredMessageRepository deferredMessageRepository;

    @MockitoBean
    DeferredMessageSender deferredMessageSenderMock;

    @MockitoBean
    ContractsValidator contractsValidator;

    @MockitoBean
    OutboxTracing outboxTracing;

    @MockitoBean
    KafkaMessagingMetrics kafkaMessagingMetrics;

    @MockitoBean
    @SuppressWarnings("unused")
    SignaturePublisherProperties signaturePublisherProperties;

    @Commit
    @Transactional
    @Test
    void testRelay() {
        DeferredMessageTestUtil.with(deferredMessageRepository).deleteAllMessagesAfter(() -> {
            when(outboxTracing.retrieveCurrentTraceContext()).thenReturn(OutboxTraceContext.builder().traceId(1L).spanId(2L).parentSpanId(3L).build());
            assertThat(deferredMessageRepository.findAll()).isEmpty();
            IntStream.range(1, 9).mapToObj(i -> StringMessage.from("test-message-" + i))
                    .forEach(message -> transactionalOutbox.sendMessageScheduled(message, "test-topic"));
            final ZonedDateTime afterSend = ZonedDateTime.now();
            TestTransaction.end();
            doAnswer(invocation -> {
                Thread.sleep(500);
                return null;
            }).when(deferredMessageSenderMock).sendAsScheduled(any(DeferredMessage.class));

            await().atMost(Duration.ofSeconds(10)).until(() ->
                    deferredMessageRepository.findMessagesReadyToBeSent(1).isEmpty()
            );

            TestTransaction.start();
            List<DeferredMessage> allDeferredMessages = deferredMessageRepository.findAll();
            allDeferredMessages.forEach(deferredMessage -> {
                assertThat(deferredMessage.getSentScheduled()).isNotNull();
                assertThat(deferredMessage.getSentScheduled()).isAfterOrEqualTo(afterSend);
                assertThat(deferredMessage.getSentScheduled()).isBeforeOrEqualTo(ZonedDateTime.now());
                assertThat(deferredMessage.getTraceContext().getTraceId()).isEqualTo(1L);
                assertThat(deferredMessage.getTraceContext().getSpanId()).isEqualTo(2L);
                assertThat(deferredMessage.getTraceContext().getParentSpanId()).isEqualTo(3L);
            });
            TestTransaction.end();
        });
    }

    @Commit
    @Transactional
    @Test
    void testRelay_withoutClusterName() {
        DeferredMessageTestUtil.with(deferredMessageRepository).deleteAllMessagesAfter(() -> {
            assertThat(deferredMessageRepository.findAll()).isEmpty();

            StringMessage message = StringMessage.from("test");
            DeferredMessage deferredMessage = DeferredMessage.builder()
                    .message(new byte[]{1, 2, 3})
                    .key(null)
                    .clusterName(null)
                    .topic("the-topic")
                    .messageId(message.getMessageId())
                    .messageIdempotenceId(message.getIdentity().getIdempotenceId())
                    .messageTypeName(message.getType().getName())
                    .messageTypeVersion(message.getType().getVersion())
                    .sendImmediately(false)
                    .traceContext(null)
                    .build();
            deferredMessageRepository.save(deferredMessage);
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).until(() ->
                    deferredMessageRepository.findMessagesReadyToBeSent(1).isEmpty()
            );

            TestTransaction.start();
            List<DeferredMessage> allDeferredMessages = deferredMessageRepository.findAll();
            assertThat(allDeferredMessages)
                    .hasSize(1);
            DeferredMessage sentMessage = allDeferredMessages.get(0);
            assertThat(sentMessage.getMessageIdempotenceId()).isEqualTo(deferredMessage.getMessageIdempotenceId());
            assertThat(sentMessage.getSentScheduled()).isNotNull();
            assertThat(sentMessage.getFailed()).isNull();
            assertThat(sentMessage.getFailReason()).isNull();
            TestTransaction.end();
        });
    }

    @Commit
    @Transactional
    @Test
    void testRelayWithFailedMessage() {
        DeferredMessageTestUtil.with(deferredMessageRepository).deleteAllMessagesAfter(() -> {
            when(outboxTracing.retrieveCurrentTraceContext()).thenReturn(OutboxTraceContext.builder().traceId(1L).spanId(2L).parentSpanId(3L).build());
            assertThat(deferredMessageRepository.findAll()).isEmpty();
            final String unauthorizedTopic = "unauthorized-topic";
            final String authorizedTopic = "authorized-topic";
            final StringMessage unauthorizedTestMessage = StringMessage.from("unauthorized-test-message");
            final StringMessage authorizedTestMessage1 = StringMessage.from("authorized-test-message-1");
            final StringMessage authorizedTestMessage2 = StringMessage.from("authorized-test-message-2");
            Mockito.doAnswer(invocation -> {
                DeferredMessage deferredMessage = invocation.getArgument(0, DeferredMessage.class);
                // Make messages to the unauthorized topic fail with a topic authorization exception
                if (unauthorizedTopic.equals(deferredMessage.getTopic())) {
                    throw DeferredMessageSendException.topicAuthorizationException(deferredMessage, new RuntimeException("unauthorized on topic"));
                } else {
                    // Let all other messages pass successfully
                    return null;
                }
            }).when(deferredMessageSenderMock).sendAsScheduled(any(DeferredMessage.class));

            transactionalOutbox.sendMessageScheduled(unauthorizedTestMessage, unauthorizedTopic);
            transactionalOutbox.sendMessageScheduled(authorizedTestMessage1, authorizedTopic);
            transactionalOutbox.sendMessageScheduled(authorizedTestMessage2, authorizedTopic);

            final ZonedDateTime afterSend = ZonedDateTime.now();
            TestTransaction.end();
            await().atMost(Duration.ofSeconds(5)).until(() ->
                    deferredMessageRepository.findMessagesReadyToBeSent(1).isEmpty()
            );
            TestTransaction.start();
            List<DeferredMessage> allDeferredMessages = deferredMessageRepository.findAll();
            allDeferredMessages.sort(Comparator.comparing(DeferredMessage::getId));
            assertThat(allDeferredMessages.get(0).getSentScheduled()).isNull();
            assertThat(allDeferredMessages.get(0).getFailed()).isNotNull();
            assertThat(allDeferredMessages.get(0).getFailed()).isAfterOrEqualTo(afterSend);
            assertThat(allDeferredMessages.get(1).getFailed()).isNull();
            assertThat(allDeferredMessages.get(1).getSentScheduled()).isNotNull();
            assertThat(allDeferredMessages.get(1).getSentScheduled()).isAfterOrEqualTo(afterSend);
            assertThat(allDeferredMessages.get(2).getFailed()).isNull();
            assertThat(allDeferredMessages.get(2).getSentScheduled()).isNotNull();
            assertThat(allDeferredMessages.get(2).getSentScheduled()).isAfterOrEqualTo(afterSend);
            TestTransaction.end();
        });
    }

}
