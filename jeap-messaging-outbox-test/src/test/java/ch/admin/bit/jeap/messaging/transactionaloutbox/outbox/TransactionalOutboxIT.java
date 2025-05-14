package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import brave.kafka.clients.KafkaTracing;
import brave.propagation.TraceContext;
import ch.admin.bit.jeap.messaging.kafka.KafkaConfiguration;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestEvent;
import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestMessageKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SuppressWarnings("SameParameterValue")
@DirtiesContext
@AutoConfigureObservability
@SpringBootTest(properties = {"jeap.messaging.kafka.exposeMessageKeyToConsumer=true"})
@Slf4j
class TransactionalOutboxIT extends KafkaIntegrationTestBase {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    DeferredMessageRepository deferredMessageRepository;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private TransactionalOutbox transactionalOutbox;
    @SuppressWarnings("unused")
    @MockitoBean
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator
    @MockitoBean
    private TestEventListener testEventListener;
    @MockitoBean
    private JeapKafkaMessageCallback jeapKafkaMessageCallback;
    @Captor
    private ArgumentCaptor<TestEvent> testEventArgumentCaptor;
    @Captor
    private ArgumentCaptor<TestMessageKey> testMessageKeyArgumentCaptor;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private KafkaConfiguration kafkaConfiguration;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private KafkaTracing kafkaTracing;
    @Autowired
    DeferredMessageTestUtil deferredMessageTestUtil;


    @AfterEach
    void cleanUp() {
        deferredMessageTestUtil.deleteAllMessages();
    }

    @Transactional
    @Commit
    @Test
    void testOutboxSend() {

        // Start tracing explicitly because the spring test transaction manager doesn't seem to get instrumented by Brave.
        // Normally, the platform transaction manager is instrumented by brave as TracePlatformTransactionManager, but in
        // the spring test transaction case the transaction manager does not get instrumented and stays a JpaTransactionManager.
        // Therefore, tracing must be started explicitly in this test as we want to test trace context propagation in this test, too.
        kafkaTracing.messagingTracing().tracing().tracer().startScopedSpan("junit");
        final TraceContext originalTraceContext = kafkaTracing.messagingTracing().tracing().currentTraceContext().get();

        assertThat(deferredMessageRepository.findAll()).isEmpty();

        final String idempotenceIdEvent1 = "idempotenceId1";
        final String idempotenceIdEvent2 = "idempotenceId2";
        final TestEvent testEvent1 = TestEventBuilder.create().idempotenceId(idempotenceIdEvent1).build();
        final TestEvent testEvent2 = TestEventBuilder.create().idempotenceId(idempotenceIdEvent2).build();
        final TestMessageKey testMessageKey = TestMessageKey.newBuilder().setSomeProperty("someValue").build();

        transactionalOutbox.sendMessage(testEvent1, testMessageKey, TestEventConsumer.TOPIC);
        transactionalOutbox.sendMessageScheduled(testEvent2, TestEventConsumer.TOPIC);

        TestTransaction.end();
        verify(testEventListener, timeout(TEST_TIMEOUT).times(2)).receive(testEventArgumentCaptor.capture(), testMessageKeyArgumentCaptor.capture());
        final List<TestEvent> receivedEvents = testEventArgumentCaptor.getAllValues();
        assertThat(receivedEvents.stream().map(event -> event.getIdentity().getIdempotenceId()).collect(Collectors.toList()))
                .containsOnly(idempotenceIdEvent1, idempotenceIdEvent2);
        final List<TestMessageKey> receivedKeys = testMessageKeyArgumentCaptor.getAllValues();
        assertThat(receivedKeys).containsOnly(testMessageKey, null);

        // Make sure the relay process finished sending the scheduled message
        deferredMessageTestUtil.waitTillAllMessagesProcessed();

        TestTransaction.start();
        final List<DeferredMessage> deferredMessages = deferredMessageRepository.findAll();
        assertThat(deferredMessages).hasSize(2);
        assertThat(deferredMessages.stream().map(DeferredMessage::getMessageIdempotenceId).collect(Collectors.toList()))
                .containsOnly(idempotenceIdEvent1, idempotenceIdEvent2);
        assertThat(deferredMessages.get(0).getSentImmediately()).isNotNull();
        assertThat(deferredMessages.get(0).getFailed()).isNull();
        assertThat(deferredMessages.get(1).getSentScheduled()).isNotNull();
        assertThat(deferredMessages.get(1).getFailed()).isNull();
        deferredMessageRepository.deleteMessagesSentBefore(ZonedDateTime.now());
        TestTransaction.end();

        final Long traceId = deferredMessages.get(0).getTraceContext().getTraceId();
        final String traceIdString = deferredMessages.get(0).getTraceContext().getTraceIdString();

        assertThat(deferredMessages.get(0).getTraceContext().getTraceIdHigh()).isEqualTo(originalTraceContext.traceIdHigh());
        assertThat(traceId).isEqualTo(originalTraceContext.traceId());
        assertThat(deferredMessages.get(0).getTraceContext().getParentSpanId()).isEqualTo(originalTraceContext.parentId());
        assertThat(deferredMessages.get(0).getTraceContext().getSpanId()).isEqualTo(originalTraceContext.spanId());
        assertThat(traceIdString).isEqualTo(originalTraceContext.traceIdString());
        assertThat(deferredMessages.stream().filter(dm -> dm.getTraceContext().getTraceId().equals(traceId)).count()).isEqualTo(2);
        assertHeaderFromConsumedMessage(traceIdString, 2);

        verify(jeapKafkaMessageCallback).onSend(testEvent1, TestEventConsumer.TOPIC);
        verify(jeapKafkaMessageCallback).onSend(testEvent2, TestEventConsumer.TOPIC);
    }

    @Transactional
    @Commit
    @Test
    void testMessageRelay_assertMessageSavedForUnknownClusterIsProducedToTheDefaultCluster() {
        // send a message with the outbox (scheduled)
        assertThat(deferredMessageRepository.findAll()).isEmpty();
        final String idempotenceIdEvent = UUID.randomUUID().toString();
        final TestEvent testEvent = TestEventBuilder.create().idempotenceId(idempotenceIdEvent).build();
        transactionalOutbox.sendMessageScheduled(testEvent, TestEventConsumer.TOPIC);
        TestTransaction.end();

        // Make sure the relay process finished sending the scheduled message
        deferredMessageTestUtil.waitTillAllMessagesProcessed();

        // Alter the message in the database to set the cluster name to 'unknown' and to clear the sent flag in order
        // to make the relay process send the message again, this time for the cluster named 'unknown'.
        TestTransaction.start();
        deferredMessageTestUtil.alterClusterNameAndMakeAvailableForMessageRelay(idempotenceIdEvent, "unknown");
        TestTransaction.end();

        // Make sure the relay process finished sending the altered scheduled message
        deferredMessageTestUtil.waitTillAllMessagesProcessed();

        // Verify that two events have been sent: the one for the default cluster (named 'default') and the one
        // for the non-existing 'unknown' cluster.
        verify(testEventListener, timeout(TEST_TIMEOUT).times(2)).receive(testEventArgumentCaptor.capture(), testMessageKeyArgumentCaptor.capture());
    }


    private void assertHeaderFromConsumedMessage(String traceIdString, int count) {
        final Consumer<Object, Object> consumer = createConsumer(kafkaConfiguration);
        consumer.assign(Collections.singletonList(new TopicPartition(TestEventConsumer.TOPIC, 0)));
        consumer.seekToBeginning(Collections.singletonList(new TopicPartition(TestEventConsumer.TOPIC, 0)));
        final ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(10));
        consumer.close();

        for (ConsumerRecord<Object, Object> record : records) {
            final String headerTraceId = (new String(record.headers().lastHeader("traceparent").value())).split("-")[1];
            assertThat(headerTraceId).isEqualTo(traceIdString);
        }
        assertThat(records.count()).isEqualTo(count);
    }

    private static Consumer<Object, Object> createConsumer(KafkaConfiguration kafkaConfiguration) {
        Map<String, Object> props = new HashMap<>(kafkaConfiguration.consumerConfig(KafkaProperties.DEFAULT_CLUSTER));
        // We are resending messages exactly as received i.e. as byte array of the original message
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        // Remove the interceptor configured by the domain event library, because it expects messages to be domain events.
        props.remove(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG);
        return new KafkaConsumer<>(props);
    }
}
