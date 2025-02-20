package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.metrics.KafkaMessagingMetrics;
import ch.admin.bit.jeap.messaging.kafka.signature.publisher.SignaturePublisherProperties;
import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport.DeferredMessageTestUtil;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport.OutboxMockKafkaNoSchedulingTestConfig;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport.StringMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@Transactional
@DataJpaTest
@ContextConfiguration(classes = OutboxMockKafkaNoSchedulingTestConfig.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class TransactionalOutboxMockKafkaIT {

    private static final String TOPIC_1 = "test-topic-1";
    private static final String TOPIC_2 = "test-topic-2";
    private static final String TOPIC_3 = "test-topic-3";
    private static final StringMessage TEST_MESSAGE_1 = StringMessage.from("test-message-1");
    private static final StringMessage TEST_MESSAGE_2 = StringMessage.from("test-message-2");
    private static final StringMessage TEST_MESSAGE_3 = StringMessage.from("test-message-3");
    private static final StringMessage TEST_MESSAGE_4 = StringMessage.from("test-message-4");
    private static final Object TEST_KEY_1 = "test-key-1";
    private static final Object TEST_KEY_2 = "test-key-2";
    private static final Object TEST_KEY_3 = "test-key-3";

    @MockitoBean
    OutboxTracing outboxTracing;

    @Autowired
    TransactionalOutbox transactionalOutbox;

    @Autowired
    MessageSerializer messageSerializer;

    @Autowired
    DeferredMessageRepository deferredMessageRepository;

    @Autowired
    TransactionalOutboxConfiguration config;

    @MockitoBean
    DeferredMessageSender deferredMessageSenderMock;

    @MockitoBean
    ContractsValidator contractsValidator;

    @Captor
    ArgumentCaptor<DeferredMessage> deferredMessageCaptor;

    @MockitoBean
            @SuppressWarnings("unused")
    KafkaMessagingMetrics kafkaMessagingMetrics;

    @MockitoBean
    @SuppressWarnings("unused")
    SignaturePublisherProperties signaturePublisherProperties;

    @BeforeEach
    void setup() {
        when(outboxTracing.retrieveCurrentTraceContext()).thenReturn(OutboxTraceContext.builder().traceId(1L).spanId(2L).parentSpanId(3L).build());
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testSendOneMessage_WhenNoTransactionActive_ThenThrowsException() {
        assertThat(TestTransaction.isActive()).isFalse();

        assertThatThrownBy(() -> transactionalOutbox.sendMessage(TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1));
    }

    @Test
    void testSendOneMessage_WhenMessageSentImmediatelyInTransaction_ThenPersistedAsDeferredMessageWithinCurrentTransaction() {
        assertThat(deferredMessageRepository.findAll()).isEmpty();
        final ZonedDateTime beforeSend = ZonedDateTime.now();

        transactionalOutbox.sendMessage(TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1);

        final ZonedDateTime afterSend = ZonedDateTime.now();
        final List<DeferredMessage> allMessages = deferredMessageRepository.findAll();
        assertThat(allMessages).hasSize(1);
        final DeferredMessage sentMessage = allMessages.get(0);
        assertDeferredMessage(sentMessage, TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1, true, 1, beforeSend, afterSend, null, false);
    }

    @Test
    void testSendOneMessage_WhenMessageSentScheduledInTransaction_ThenPersistedAsDeferredMessageWithinCurrentTransaction() {
        assertThat(deferredMessageRepository.findAll()).isEmpty();
        final ZonedDateTime beforeSend = ZonedDateTime.now();

        transactionalOutbox.sendMessageScheduled(TEST_MESSAGE_2, TEST_KEY_2, TOPIC_2);

        final ZonedDateTime afterSend = ZonedDateTime.now();
        final List<DeferredMessage> allMessages = deferredMessageRepository.findAll();
        assertThat(allMessages).hasSize(1);
        final DeferredMessage sentMessage = allMessages.get(0);
        assertDeferredMessage(sentMessage, TEST_MESSAGE_2, TEST_KEY_2, TOPIC_2, false, 0, beforeSend, afterSend, null, false);
    }


    @Test
    void testSendThreeMessage_WhenMessagesSentInTransaction_ThenPersistedAsDeferredMessagesWithinCurrentTransaction() {
        assertThat(deferredMessageRepository.findAll()).isEmpty();
        final ZonedDateTime beforeSend = ZonedDateTime.now();

        transactionalOutbox.sendMessage(TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1);
        transactionalOutbox.sendMessageScheduled(TEST_MESSAGE_2, null, TOPIC_2);
        transactionalOutbox.sendMessage(TEST_MESSAGE_3, TEST_KEY_1, TOPIC_1);

        final ZonedDateTime afterSend = ZonedDateTime.now();
        final List<DeferredMessage> allMessages = deferredMessageRepository.findAll();
        allMessages.sort(Comparator.comparing(DeferredMessage::getCreated));
        assertThat(allMessages).hasSize(3);
        assertDeferredMessage(allMessages.get(0), TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1, true, 2, beforeSend, afterSend, null, false);
        assertDeferredMessage(allMessages.get(1), TEST_MESSAGE_2, null, TOPIC_2, false, 2, beforeSend, afterSend, null, false);
        assertDeferredMessage(allMessages.get(2), TEST_MESSAGE_3, TEST_KEY_1, TOPIC_1, true, 2, beforeSend, afterSend, null, false);
    }

    @Test
    void testSendMessage_WhenMessageSentInTransactionWithRollback_ThenNotSentAsDeferredMessageAfterTransactionEnd() {
        transactionalOutbox.sendMessage(TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1);
        TestTransaction.end();
        verifyNoInteractions(deferredMessageSenderMock);
    }

    @Commit
    @Test
    void testSendOneMessage_WhenMessageSentImmediatelyInTransaction_ThenSentAsDeferredMessageAndMarkedSentImmediatelyAfterTransactionCommit() {
        DeferredMessageTestUtil.with(deferredMessageRepository).deleteAllMessagesAfter(() -> {
            assertThat(deferredMessageRepository.findAll()).isEmpty();
            final ZonedDateTime beforeSend = ZonedDateTime.now();

            transactionalOutbox.sendMessage(TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1);

            final ZonedDateTime afterSend = ZonedDateTime.now();
            verifyNoInteractions(deferredMessageSenderMock);
            TestTransaction.end();
            final ZonedDateTime afterCommit = ZonedDateTime.now();
            verify(deferredMessageSenderMock, times(1)).sendAsImmediate(deferredMessageCaptor.capture());
            verifyNoMoreInteractions(deferredMessageSenderMock);
            final DeferredMessage deferredMessage = deferredMessageCaptor.getValue();
            assertDeferredMessage(deferredMessage, TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1, true, 1, beforeSend, afterSend, null, false);

            TestTransaction.start();
            final List<DeferredMessage> allMessages = deferredMessageRepository.findAll();
            assertThat(allMessages).hasSize(1);
            final DeferredMessage sentMessage = allMessages.get(0);
            assertDeferredMessage(sentMessage, TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1, true, 1, beforeSend, afterSend, afterCommit, false);
            TestTransaction.end();
        });
    }

    @Commit
    @Test
    void testSendOneMessage_WhenMessageSentScheduledInTransaction_ThenNotImmediatelySentAfterTransactionCommit() {
        DeferredMessageTestUtil.with(deferredMessageRepository).deleteAllMessagesAfter(() -> {
            assertThat(deferredMessageRepository.findAll()).isEmpty();
            final ZonedDateTime beforeSend = ZonedDateTime.now();

            transactionalOutbox.sendMessageScheduled(TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1);

            final ZonedDateTime afterSend = ZonedDateTime.now();
            verifyNoInteractions(deferredMessageSenderMock);

            TestTransaction.end();
            verifyNoInteractions(deferredMessageSenderMock);

            TestTransaction.start();
            final List<DeferredMessage> allMessages = deferredMessageRepository.findAll();
            assertThat(allMessages).hasSize(1);
            final DeferredMessage sentMessage = allMessages.get(0);
            assertDeferredMessage(sentMessage, TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1, false, 0, beforeSend, afterSend, null, false);
            TestTransaction.end();
        });
    }

    @Commit
    @Test
    void testSendThreeMessages_WhenTwoMessagesSentImmediatelyInTransaction_ThenTwoSentAsDeferredMessagesAndMarkedSentImmediatelyAfterTransactionCommit() {
        DeferredMessageTestUtil.with(deferredMessageRepository).deleteAllMessagesAfter(() -> {
            assertThat(deferredMessageRepository.findAll()).isEmpty();
            final ZonedDateTime beforeSend = ZonedDateTime.now();

            transactionalOutbox.sendMessage(TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1);
            transactionalOutbox.sendMessageScheduled(TEST_MESSAGE_2, TOPIC_2);
            transactionalOutbox.sendMessage(TEST_MESSAGE_3, TOPIC_1);

            final ZonedDateTime afterSend = ZonedDateTime.now();
            verifyNoInteractions(deferredMessageSenderMock);
            TestTransaction.end();
            final ZonedDateTime afterCommit = ZonedDateTime.now();

            verify(deferredMessageSenderMock, times(2)).sendAsImmediate(deferredMessageCaptor.capture());
            final List<DeferredMessage> sentImmediatelyMessages = deferredMessageCaptor.getAllValues();
            assertThat(sentImmediatelyMessages.size()).isEqualTo(2);
            assertDeferredMessage(sentImmediatelyMessages.get(0), TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1, true, 2, beforeSend, afterSend, null, false);
            assertDeferredMessage(sentImmediatelyMessages.get(1), TEST_MESSAGE_3, null, TOPIC_1, true, 2, beforeSend, afterSend, null, false);

            TestTransaction.start();
            final List<DeferredMessage> sentMessages = deferredMessageRepository.findAll();
            sentMessages.sort(Comparator.comparing(DeferredMessage::getCreated));
            assertThat(sentMessages).hasSize(3);
            assertDeferredMessage(sentMessages.get(0), TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1, true, 2, beforeSend, afterSend, afterCommit, false);
            assertDeferredMessage(sentMessages.get(1), TEST_MESSAGE_2, null, TOPIC_2, false, 2, beforeSend, afterSend, null, false);
            assertDeferredMessage(sentMessages.get(2), TEST_MESSAGE_3, null, TOPIC_1, true, 2, beforeSend, afterSend, afterCommit, false);
            TestTransaction.end();
        });
    }

    @Commit
    @Test
    void testSendOneMessage_WhenDeferredMessageSendingThrowsException_DeferredMessageHasBeenPersistedButNotMarkedSent() {
        DeferredMessageTestUtil.with(deferredMessageRepository).deleteAllMessagesAfter(() -> {
            assertThat(deferredMessageRepository.findAll()).isEmpty();
            final ZonedDateTime beforeSend = ZonedDateTime.now();
            Mockito.doThrow(TransactionalOutboxException.class).when(deferredMessageSenderMock).sendAsImmediate(any(DeferredMessage.class));

            transactionalOutbox.sendMessage(TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1);

            verifyNoInteractions(deferredMessageSenderMock);
            TestTransaction.end();
            final ZonedDateTime afterSend = ZonedDateTime.now();
            verify(deferredMessageSenderMock, times(1)).sendAsImmediate(deferredMessageCaptor.capture());
            verifyNoMoreInteractions(deferredMessageSenderMock);
            final DeferredMessage deferredMessage = deferredMessageCaptor.getValue();
            assertDeferredMessage(deferredMessage, TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1, true, 1, beforeSend, afterSend, null, false);

            TestTransaction.start();
            final List<DeferredMessage> allMessages = deferredMessageRepository.findAll();
            assertThat(allMessages).hasSize(1);
            final DeferredMessage sentMessage = allMessages.get(0);
            assertDeferredMessage(sentMessage, TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1, true, 1, beforeSend, afterSend, null, false);
            TestTransaction.end();
        });
    }

    @Commit
    @Test
    void testSendMessages_WhenDeferredMessageSendingThrowsExceptions_ThenHandledCorrectly() {
        DeferredMessageTestUtil.with(deferredMessageRepository).deleteAllMessagesAfter(() -> {
            assertThat(deferredMessageRepository.findAll()).isEmpty();
            Mockito.doAnswer(invocation -> {
                DeferredMessage deferredMessage = invocation.getArgument(0, DeferredMessage.class);
                // Make TEST_MESSAGE_1 fail because of a TopicAuthorizationException
                if (Arrays.equals(deferredMessage.getMessage(), messageSerializer.serializeMessage(TEST_MESSAGE_1, TOPIC_1))) {
                    throw DeferredMessageSendException.topicAuthorizationException(deferredMessage, new RuntimeException("unauthorized on topic"));
                }
                // Let TEST_MESSAGE_2 pass successfully
                else if (Arrays.equals(deferredMessage.getMessage(), messageSerializer.serializeMessage(TEST_MESSAGE_2, TOPIC_2))) {
                    return null;
                }
                // Make TEST_MESSAGE_3 throw a general sending exception that will not mark the message as failed
                else if (Arrays.equals(deferredMessage.getMessage(), messageSerializer.serializeMessage(TEST_MESSAGE_3, TOPIC_3))) {
                    throw DeferredMessageSendException.generalSendException(deferredMessage, new RuntimeException("oops"));
                } else {
                    // Let all other messages pass successfully
                    return null;
                }
            }).when(deferredMessageSenderMock).sendAsImmediate(any(DeferredMessage.class));
            final ZonedDateTime beforeSend = ZonedDateTime.now();

            transactionalOutbox.sendMessage(TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1);
            transactionalOutbox.sendMessage(TEST_MESSAGE_2, TEST_KEY_2, TOPIC_2);
            transactionalOutbox.sendMessage(TEST_MESSAGE_3, TEST_KEY_3, TOPIC_3);
            transactionalOutbox.sendMessage(TEST_MESSAGE_4, TOPIC_1);
            final ZonedDateTime afterSend = ZonedDateTime.now();

            verifyNoInteractions(deferredMessageSenderMock);
            TestTransaction.end();
            final ZonedDateTime afterCommit = ZonedDateTime.now();
            verify(deferredMessageSenderMock, times(3)).sendAsImmediate(deferredMessageCaptor.capture());
            verifyNoMoreInteractions(deferredMessageSenderMock);
            final List<DeferredMessage> deferredMessages = deferredMessageCaptor.getAllValues();
            assertDeferredMessage(deferredMessages.get(0), TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1, true, 4, beforeSend, afterSend, null, false);
            assertDeferredMessage(deferredMessages.get(1), TEST_MESSAGE_2, TEST_KEY_2, TOPIC_2, true, 4, beforeSend, afterSend, null, false);
            assertDeferredMessage(deferredMessages.get(2), TEST_MESSAGE_3, TEST_KEY_3, TOPIC_3, true, 4, beforeSend, afterSend, null, false);


            TestTransaction.start();
            final List<DeferredMessage> allMessages = deferredMessageRepository.findAll();
            allMessages.sort(Comparator.comparing(DeferredMessage::getCreated));
            assertThat(allMessages).hasSize(4);
            // TEST_MESSAGE_1 failed
            assertDeferredMessage(allMessages.get(0), TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1, true, 4, beforeSend, afterSend, null, true);
            // TEST_MESSAGE_2 sent
            assertDeferredMessage(allMessages.get(1), TEST_MESSAGE_2, TEST_KEY_2, TOPIC_2, true, 4, beforeSend, afterSend, afterCommit, false);
            // TEST_MESSAGE_3 unsent because of error
            assertDeferredMessage(allMessages.get(2), TEST_MESSAGE_3, TEST_KEY_3, TOPIC_3, true, 4, beforeSend, afterSend, null, false);
            // TEST_MESSAGE_4 skipped
            assertDeferredMessage(allMessages.get(3), TEST_MESSAGE_4, null, TOPIC_1, true, 4, beforeSend, afterSend, null, false);
            TestTransaction.end();
        });
    }

    @Commit
    @Test
    void testSendFailResendMessages() {
        DeferredMessageTestUtil.with(deferredMessageRepository).deleteAllMessagesAfter(() -> {
            assertThat(deferredMessageRepository.findAll()).isEmpty();
            Mockito.doAnswer(invocation -> {
                DeferredMessage deferredMessage = invocation.getArgument(0, DeferredMessage.class);
                // Make TEST_MESSAGE_1  and TEST_MESSAGE_2 fail because of a TopicAuthorizationException
                if (Arrays.equals(deferredMessage.getMessage(), messageSerializer.serializeMessage(TEST_MESSAGE_1, TOPIC_1)) ||
                        Arrays.equals(deferredMessage.getMessage(), messageSerializer.serializeMessage(TEST_MESSAGE_2, TOPIC_2))) {
                    throw DeferredMessageSendException.topicAuthorizationException(deferredMessage, new RuntimeException("unauthorized on topic"));
                } else {
                    // Let all other messages pass successfully
                    return null;
                }
            }).when(deferredMessageSenderMock).sendAsScheduled(any(DeferredMessage.class));
            final ZonedDateTime beforeSend = ZonedDateTime.now();
            transactionalOutbox.sendMessageScheduled(TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1);
            transactionalOutbox.sendMessageScheduled(TEST_MESSAGE_2, TEST_KEY_2, TOPIC_2);
            transactionalOutbox.sendMessageScheduled(TEST_MESSAGE_3, TEST_KEY_3, TOPIC_3);

            // make messages visible to relay process and wait for them to be sent
            TestTransaction.end();
            verify(deferredMessageSenderMock, timeout(5000).times(3)).sendAsScheduled(deferredMessageCaptor.capture());
            verifyNoMoreInteractions(deferredMessageSenderMock);

            // wait for relay send transaction to be committed
            DeferredMessageTestUtil.with(deferredMessageRepository).waitForMessageSentOrFailed(deferredMessageCaptor.getValue().getId());

            TestTransaction.start();
            final ZonedDateTime afterDelivery = ZonedDateTime.now();
            assertFailedMessages(beforeSend, afterDelivery, 2, 0);
            FailedMessage failedMessage = transactionalOutbox.findFailedMessages(0, afterDelivery, false, 1).get(0);


            // Reset mock to no longer fail previously failed messages
            Mockito.reset(deferredMessageSenderMock);

            transactionalOutbox.resendMessageScheduled(failedMessage.getId());

            assertFailedMessages(beforeSend, afterDelivery, 1, 1);

            // make message marked resend visible to relay process and wait for it to be sent
            TestTransaction.end();
            verify(deferredMessageSenderMock, timeout(5000).times(1)).sendAsScheduled(deferredMessageCaptor.capture());
            verifyNoMoreInteractions(deferredMessageSenderMock);

            // Wait for relay process send transaction to be committed
            DeferredMessageTestUtil.with(deferredMessageRepository).waitForMessageSentOrFailed(deferredMessageCaptor.getValue().getId());

            TestTransaction.start();
            assertFailedMessages(beforeSend, afterDelivery, 1, 0);
            TestTransaction.end();
        });
    }

    private void assertFailedMessages(ZonedDateTime startingFrom, ZonedDateTime before, int expectedNumFailedNotInResend, int expectedNumFailedInResend) {
        assertThat(transactionalOutbox.countFailedMessages(false)).isEqualTo(expectedNumFailedNotInResend);
        assertThat(transactionalOutbox.countFailedMessages(true)).isEqualTo(expectedNumFailedInResend);
        assertThat(transactionalOutbox.countFailedMessages(startingFrom, before, false)).isEqualTo(expectedNumFailedNotInResend);
        assertThat(transactionalOutbox.countFailedMessages(startingFrom, before, true)).isEqualTo(expectedNumFailedInResend);
        List<FailedMessage> failedMessages = transactionalOutbox.findFailedMessages(startingFrom, before, false, expectedNumFailedNotInResend + 1);
        assertThat(failedMessages).hasSize(expectedNumFailedNotInResend);
        List<FailedMessage> failedMessagesInResend = transactionalOutbox.findFailedMessages(startingFrom, before, true, expectedNumFailedInResend + 1);
        assertThat(failedMessagesInResend).hasSize(expectedNumFailedInResend);
        if (failedMessages.size() > 1) {
            List<FailedMessage> failedMessagesSublist = transactionalOutbox.findFailedMessages(failedMessages.get(0).getId(), before, false, failedMessages.size());
            assertThat(failedMessagesSublist).hasSize(failedMessages.size() - 1);
        }
        if (failedMessagesInResend.size() > 1) {
            List<FailedMessage> failedMessagesSublistInResend = transactionalOutbox.findFailedMessages(failedMessages.get(0).getId(), before, true, failedMessagesInResend.size());
            assertThat(failedMessagesSublistInResend).hasSize(failedMessagesSublistInResend.size() - 1);
        }
    }

    private void assertDeferredMessage(DeferredMessage deferredMessage, Message message, Object key, String topic, boolean sendImmediately, int numMessagesSentImmediately, ZonedDateTime beforeCreate, ZonedDateTime afterCreate, ZonedDateTime afterCommit, boolean failed) {
        assertThat(deferredMessage.getTopic()).isEqualTo(topic);
        assertThat(deferredMessage.getMessage()).isEqualTo(messageSerializer.serializeMessage(message, topic));
        if (key != null) {
            assertThat(deferredMessage.getKey()).isEqualTo(messageSerializer.serializeKey(key, topic));
        } else {
            assertThat(deferredMessage.getKey()).isNull();
        }
        assertThat(deferredMessage.getId()).isNotNull();
        assertThat(deferredMessage.getCreated()).isAfterOrEqualTo(beforeCreate);
        assertThat(deferredMessage.getCreated()).isBeforeOrEqualTo(afterCreate);
        assertThat(deferredMessage.isSendImmediately()).isEqualTo(sendImmediately);
        assertThat(deferredMessage.getSentScheduled()).isNull();
        assertThat(deferredMessage.getTraceContext().getTraceId()).isEqualTo(1L);
        assertThat(deferredMessage.getTraceContext().getSpanId()).isEqualTo(2L);
        assertThat(deferredMessage.getTraceContext().getParentSpanId()).isEqualTo(3L);
        if (sendImmediately && (afterCommit != null)) {
            assertThat(deferredMessage.getSentImmediately()).isAfterOrEqualTo(deferredMessage.getCreated());
            assertThat(deferredMessage.getSentImmediately()).isBeforeOrEqualTo(afterCommit);
            Duration relayDelay = getRelayDelay(numMessagesSentImmediately);
            assertThat(deferredMessage.getScheduleAfter()).isAfterOrEqualTo(afterCreate.plus(relayDelay));
            assertThat(deferredMessage.getScheduleAfter()).isBeforeOrEqualTo(afterCommit.plus(relayDelay));
        } else {
            assertThat(deferredMessage.getSentImmediately()).isNull();
        }
        if (failed) {
            assertThat(deferredMessage.getFailed()).isAfterOrEqualTo(afterCreate);
        } else {
            assertThat(deferredMessage.getFailed()).isNull();
        }
    }

    private Duration getRelayDelay(int numMessages) {
        // as calculated in DeferredMessagesSendingTxSync
        return config.getMaxDurationSendImmediately().multipliedBy(numMessages);
    }

}
