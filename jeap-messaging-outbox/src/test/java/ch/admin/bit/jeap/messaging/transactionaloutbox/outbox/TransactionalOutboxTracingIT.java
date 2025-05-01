package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.metrics.KafkaMessagingMetrics;
import ch.admin.bit.jeap.messaging.kafka.signature.publisher.SignaturePublisherProperties;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport.OutboxMockKafkaNoSchedulingTestConfig;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport.StringMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Transactional
@DataJpaTest
@ContextConfiguration(classes = OutboxMockKafkaNoSchedulingTestConfig.class)
class TransactionalOutboxTracingIT {

    private static final String TOPIC_1 = "test-topic-1";
    private static final StringMessage TEST_MESSAGE_1 = StringMessage.from("test-message-1");
    private static final Object TEST_KEY_1 = "test-key-1";

    @MockitoBean
    OutboxTracing outboxTracing;

    TransactionalOutbox transactionalOutbox;

    @Autowired
    MessageSerializer messageSerializer;

    @Autowired
    DeferredMessageRepository deferredMessageRepository;

    @Autowired
    FailedMessageRepository failedMessageRepository;

    @Autowired
    TransactionalOutboxConfiguration config;

    @Autowired
    TestEntityManager testEntityManager;

    @MockitoBean
    DeferredMessageSender deferredMessageSenderMock;

    @MockitoBean
    ContractsValidator contractsValidator;

    @Autowired
    AfterCommitMessageSender afterCommitMessageSender;

    @MockitoBean
    KafkaMessagingMetrics kafkaMessagingMetrics;

    @MockitoBean
    @SuppressWarnings("unused")
    SignaturePublisherProperties signaturePublisherProperties;

    @Test
    void testSend_WhenTraceContextProviderNotPresent_ThenNoTraceInDeferredMessage() {
        transactionalOutbox = new TransactionalOutbox("testclustername", messageSerializer,
                deferredMessageRepository, failedMessageRepository, afterCommitMessageSender,
                contractsValidator, Optional.empty(), outboxTracing, List.of());
        assertThat(deferredMessageRepository.findAll()).isEmpty();

        transactionalOutbox.sendMessage(TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1);

        final List<DeferredMessage> allMessages = deferredMessageRepository.findAll();
        assertThat(allMessages).hasSize(1);
        final DeferredMessage sentMessage = allMessages.get(0);

        assertThat(sentMessage.getTraceContext()).isNull();
        deleteMessages(allMessages);
    }

    @Test
    void testSend_WhenTraceContextProviderIsPresent_ThenTraceInDeferredMessage() {
        when(outboxTracing.retrieveCurrentTraceContext()).thenReturn(OutboxTraceContext.builder()
                .spanId(2L)
                .traceId(1L)
                .parentSpanId(3L)
                .build());
        transactionalOutbox = new TransactionalOutbox("testclustername", messageSerializer,
                deferredMessageRepository, failedMessageRepository, afterCommitMessageSender,
                contractsValidator, Optional.empty(), outboxTracing, List.of());
        assertThat(deferredMessageRepository.findAll()).isEmpty();

        transactionalOutbox.sendMessage(TEST_MESSAGE_1, TEST_KEY_1, TOPIC_1);

        final List<DeferredMessage> allMessages = deferredMessageRepository.findAll();
        assertThat(allMessages).hasSize(1);
        final DeferredMessage sentMessage = allMessages.get(0);

        assertThat(sentMessage.getTraceContext().getTraceId()).isEqualTo(1L);
        assertThat(sentMessage.getTraceContext().getSpanId()).isEqualTo(2L);
        assertThat(sentMessage.getTraceContext().getParentSpanId()).isEqualTo(3L);
        deleteMessages(allMessages);
    }

    private void deleteMessages(Collection<DeferredMessage> deferredMessages) {
        deferredMessages.forEach(m -> testEntityManager.remove(m));
    }
}
