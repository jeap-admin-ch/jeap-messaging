package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContext;
import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContextUpdater;
import ch.admin.bit.jeap.messaging.sequentialinbox.inbox.SequentialInboxService;
import ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.MessageRecorder;
import ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message.MultipleTestEventListener;
import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.MessageRepository;
import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.SequenceInstanceRepository;
import ch.admin.bit.jeap.messaging.sequentialinbox.kafka.KafkaSequentialInboxMessageConsumerFactory;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.*;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"SqlResolve", "DataFlowIssue", "SqlNoDataSourceInspection", "SameParameterValue"})
@AutoConfigureObservability
@SpringBootTest
@Testcontainers
@Slf4j
@DirtiesContext
class SequentialInboxITBase extends KafkaIntegrationTestBase {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MessageRecorder messageRecorder;

    @Autowired
    MultipleTestEventListener testEventListener;

    @Autowired
    private KafkaSequentialInboxMessageConsumerFactory kafkaSequentialInboxMessageConsumerFactory;

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    SequenceInstanceRepository sequenceInstanceRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private TraceContextUpdater traceContextUpdater;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    MeterRegistry meterRegistry;

    @MockitoSpyBean
    protected SequentialInboxService sequentialInboxService;

    @BeforeAll
    static void beforeAll() {
        Awaitility.setDefaultPollDelay(Duration.ZERO);
        Awaitility.setDefaultPollInterval(Duration.ofMillis(50));
        Awaitility.setDefaultTimeout(TIMEOUT);
    }

    @AfterAll
    static void afterAll() {
        Awaitility.reset();
    }

    @BeforeEach
    void setUp() {
        MultipleTestEventListener.failOnJmeSimpleTestEvent = false;
        messageRecorder.reset();
        kafkaSequentialInboxMessageConsumerFactory.getContainers()
                .forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }

    void assertSequencedMessageProcessedSuccessfully(AvroMessage... messages) {
        Arrays.stream(messages).forEach(message ->
                await().until(() -> messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction(message.getType().getName(), message.getIdentity().getIdempotenceId())
                        .map(SequencedMessage::getState)
                        .filter(state -> state == SequencedMessageState.PROCESSED)
                        .isPresent()));
    }

    void assertSequencedMessageProcessedSuccessfully(AvroMessage message, String messageTypeQn) {
        await().until(() -> messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction(messageTypeQn, message.getIdentity().getIdempotenceId())
                .map(SequencedMessage::getState)
                .filter(state -> state == SequencedMessageState.PROCESSED)
                .isPresent());
    }

    void assertNoSequencePersistedForContextOfEvent(AvroMessage message) {
        assertThat(findSequenceInstanceByContextId(message.getOptionalProcessId().orElseThrow()))
                .describedAs("No sequence should be created for the event")
                .isEmpty();

        assertThat(messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction(message.getClass().getName(), message.getIdentity().getIdempotenceId()))
                .describedAs("No SequencedMessage should be created for the event")
                .isEmpty();
    }

    void assertMessageCountHandledByInbox(int wantedNumberOfInvocations) {
        await("Sequential inbox has handled %d messages".formatted(wantedNumberOfInvocations))
                .untilAsserted(() -> verify(sequentialInboxService, times(wantedNumberOfInvocations)).handleMessage(any(), any(), any()));
    }

    void assertMessageSentToErrorHandlingService(AvroMessage message) {
        messageRecorder.assertMessageProcessingFailedEventConsumedForMessage(message);
    }

    void assertMessageConsumedByListener(AvroMessage... messages) {
        Arrays.stream(messages).forEach(message ->
                messageRecorder.assertMessageConsumed(message.getIdentity().getId()));
    }

    void assertKeyConsumedByListener(AvroMessageKey key, AvroMessage message) {
        messageRecorder.assertKeyForMessage(key, message);
    }

    void assertMessageNotConsumedByListener(AvroMessage... messages) {
        Arrays.stream(messages).forEach(message ->
                messageRecorder.assertMessageNotConsumed(message.getIdentity().getId()));
    }

    void assertMessageStateWaitingAndBuffered(AvroMessage... message) {
        Arrays.stream(message).forEach(this::assertMessageStateWaitingAndBuffered);
    }

    void assertMessageStateWaitingAndBuffered(AvroMessage message) {
        String messageTypeName = message.getType().getName();
        assertMessageStateWaitingAndBuffered(message, messageTypeName);
    }

    void assertMessageStateWaitingAndBuffered(AvroMessage message, String messageTypeName) {
        await("SequencedMessage is present and waiting")
                .until(() -> messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction(messageTypeName, message.getIdentity().getIdempotenceId())
                        .map(SequencedMessage::getState)
                        .filter(state -> state == SequencedMessageState.WAITING)
                        .isPresent());

        SequencedMessage sequencedMessage = messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction(messageTypeName, message.getIdentity().getIdempotenceId()).orElseThrow();
        BufferedMessage bufferedMessage = messageRepository.getBufferedMessageInNewTransaction(sequencedMessage);
        assertThat(bufferedMessage.getSequencedMessageId())
                .isEqualTo(sequencedMessage.getId());
    }

    void assertMessageStateFailedAndNotBuffered(AvroMessage message) {
        assertMessageStateFailed(message);

        SequencedMessage sequencedMessage = messageRepository
                .findByMessageTypeAndIdempotenceIdInNewTransaction(message.getType().getName(), message.getIdentity().getIdempotenceId())
                .orElseThrow();
        assertThat(messageRepository.getBufferedMessageInNewTransaction(sequencedMessage))
                .describedAs("No message buffered for sequenced message " + sequencedMessage.getId())
                .isNull();
    }

    void assertMessageStateFailed(AvroMessage message) {
        await("SequencedMessage is present and marked as failed, no buffered message has been created for it")
                .until(() -> messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction(message.getType().getName(), message.getIdentity().getIdempotenceId())
                        .map(SequencedMessage::getState)
                        .filter(state -> state == SequencedMessageState.FAILED)
                        .isPresent());
    }

    void assertSequenceOfMessages(UUID contextId, AvroMessage... messages) {
        messageRecorder.assertMessageSequenceForContextId(contextId, messages);
    }

    void assertSequenceOpen(AvroMessage... messages) {
        Arrays.stream(messages).forEach(message ->
                assertSequenceState(message.getOptionalProcessId().orElseThrow(), SequenceInstanceState.OPEN));
    }

    void assertSequenceOpen(UUID contextId) {
        assertSequenceState(contextId.toString(), SequenceInstanceState.OPEN);
    }

    void assertSequenceClosed(AvroMessage avroMessage) {
        assertSequenceClosed(avroMessage.getOptionalProcessId().orElseThrow());
    }

    void assertSequenceClosed(UUID contextId) {
        assertSequenceClosed(contextId.toString());
    }

    void assertSequenceClosed(String contextId) {
        assertSequenceState(contextId, SequenceInstanceState.CLOSED);
    }

    void assertSequenceState(String contextId, SequenceInstanceState state) {
        await("Sequence in state " + state.name()).untilAsserted(() ->
                assertThat(findSequenceInstanceByContextId(contextId))
                        .hasValueSatisfying(sequenceInstance ->
                                assertThat(sequenceInstance.getState()).isSameAs(state)));
    }

    void assertBufferedMessageCount(UUID contextId, int expectedCount) {
        int bufferedMessageCount = jdbcTemplate.queryForObject(
                "select count(*) from buffered_message where sequence_instance_id = (select id from sequence_instance where context_id = ?)", Integer.class, contextId.toString());
        assertThat(bufferedMessageCount)
                .describedAs(expectedCount + " messages buffered for context " + contextId)
                .isEqualTo(expectedCount);
    }

    void assertSequencedMessageCount(UUID contextId, int expectedCount) {
        int sequencedMessageCount = jdbcTemplate.queryForObject(
                "select count(*) from sequenced_message where sequence_instance_id = (select id from sequence_instance where context_id = ?)", Integer.class, contextId.toString());
        assertThat(sequencedMessageCount)
                .describedAs("Only one SequencedMessage should be created for the event")
                .isEqualTo(expectedCount);
    }

    void assertTraceContextId(long traceId, AvroMessage message) {
        messageRecorder.assertTraceContextForMessage(traceId, message);
    }

    void setTraceContext(Long traceId) {
        traceContextUpdater.setTraceContext(new TraceContext(0L, traceId, 123L, 456L, null));
    }

    Optional<SequenceInstance> findSequenceInstanceByContextId(String contextId) {
        List<SequenceInstance> results = entityManager.createQuery("select si from SequenceInstance si where si.contextId = :contextId", SequenceInstance.class)
                .setParameter("contextId", contextId)
                .getResultList();
        return results.stream().findFirst();
    }
}
