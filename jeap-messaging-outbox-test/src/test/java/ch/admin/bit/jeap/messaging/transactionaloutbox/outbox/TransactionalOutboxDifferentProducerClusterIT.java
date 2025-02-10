package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.test.EmbeddedKafkaMultiClusterExtension;
import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TestEventConsumer.TOPIC;
import static ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutboxDifferentProducerClusterIT.PORT_OFFSET;
import static ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutboxDifferentProducerClusterIT.TestConsumer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "jeap.messaging.kafka.embedded=false",
        "jeap.messaging.kafka.systemName=test",
        "jeap.messaging.kafka.errorTopicName=errorTopic",
        "jeap.messaging.kafka.cluster.default.bootstrapServers=localhost:" + (EmbeddedKafkaMultiClusterExtension.BASE_PORT + PORT_OFFSET),
        "jeap.messaging.kafka.cluster.default.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.default.schemaRegistryUrl=mock://registry-outbox-1",
        "jeap.messaging.kafka.cluster.default.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.default.schemaRegistryPassword=unused",
        "jeap.messaging.kafka.cluster.secondcluster.default-producer-cluster-override=true",
        "jeap.messaging.kafka.cluster.secondcluster.bootstrapServers=localhost:" + (EmbeddedKafkaMultiClusterExtension.BASE_PORT + PORT_OFFSET + 1),
        "jeap.messaging.kafka.cluster.secondcluster.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.secondcluster.schemaRegistryUrl=mock://registry-outbox-2",
        "jeap.messaging.kafka.cluster.secondcluster.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.secondcluster.schemaRegistryPassword=unused"
})
@Import(TestConsumer.class)
class TransactionalOutboxDifferentProducerClusterIT {

    static final int PORT_OFFSET = 60;

    @RegisterExtension
    static EmbeddedKafkaMultiClusterExtension embeddedKafkaMultiClusterExtension =
            EmbeddedKafkaMultiClusterExtension.withPortOffset(PORT_OFFSET);

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    DeferredMessageRepository deferredMessageRepository;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private TransactionalOutbox transactionalOutboxDefaultCluster;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private KafkaListenerEndpointRegistry registry;
    @SuppressWarnings("unused")
    @MockitoBean
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private TestConsumer testConsumer;
    @Autowired
    DeferredMessageTestUtil deferredMessageTestUtil;


    @BeforeEach
    void waitForKafkaListener() {
        registry.getListenerContainers()
                .forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }

    @Slf4j
    static class TestConsumer {
        final private List<TestEvent> testEventsSecondCluster = new ArrayList<>();

        @KafkaListener(topics = TOPIC, groupId = "multicluster-it",
                containerFactory = "secondclusterKafkaListenerContainerFactory")
        public void consumeSecondCluster(@Payload TestEvent event,
                                         @Header(name = "jeapClusterName") String clusterName,
                                         Acknowledgment ack) {
            log.debug("Consuming event {} from cluster {} (msg: {}).", event.getType().getName(), clusterName, event.getPayload().getMessage());
            testEventsSecondCluster.add(event);
            ack.acknowledge();
        }
    }

    @AfterEach
    void cleanUp() {
        deferredMessageTestUtil.deleteAllMessages();
    }

    @Transactional
    @Commit
    @Test
    void testOutboxSend_assertEventIsProducedToDefaultProducerCluster() {
        assertThat(deferredMessageRepository.findAll()).isEmpty();

        TestEvent testEvent = TestEventBuilder.createWithRandomIdempotenceId().message("default-cluster-immediate").build();
        TestEvent testEventScheduled = TestEventBuilder.createWithRandomIdempotenceId().message("default-cluster-deferred").build();
        transactionalOutboxDefaultCluster.sendMessage(testEvent, TOPIC);
        transactionalOutboxDefaultCluster.sendMessageScheduled(testEventScheduled, TOPIC);
        TestTransaction.end();

        // Make sure the relay process finished sending the scheduled messages
        deferredMessageTestUtil.waitTillAllMessagesProcessed();

        assertNumberOfEventsAndIdempotenceIds(testConsumer.testEventsSecondCluster, 2, Set.of(
                testEvent.getIdentity().getIdempotenceId(), testEventScheduled.getIdentity().getIdempotenceId()));

        TestTransaction.start();
        final List<DeferredMessage> deferredMessages = deferredMessageRepository.findAll();
        assertThat(deferredMessages.stream().map(DeferredMessage::getMessageIdempotenceId).collect(Collectors.toList()))
                .containsOnly(testEvent.getIdentity().getIdempotenceId(),
                        testEventScheduled.getIdentity().getIdempotenceId());
        assertThat(deferredMessages.get(0).getSentImmediately()).isNotNull();
        assertThat(deferredMessages.get(0).getFailed()).isNull();
        assertThat(deferredMessages.get(1).getSentScheduled()).isNotNull();
        assertThat(deferredMessages.get(1).getFailed()).isNull();
        TestTransaction.end();
    }

    @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
    @Transactional
    @Commit
    @Test
    void testMessageRelay_assertMessageSavedForUnknownClusterIsProducedToTheDefaultProducerCluster() {
        assertThat(deferredMessageRepository.findAll()).isEmpty();

        // send a message with the outbox (scheduled)
        assertThat(deferredMessageRepository.findAll()).isEmpty();
        final String idempotenceIdEvent = UUID.randomUUID().toString();
        final TestEvent testEvent = TestEventBuilder.create().idempotenceId(idempotenceIdEvent).build();
        transactionalOutboxDefaultCluster.sendMessageScheduled(testEvent, TestEventConsumer.TOPIC);
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

        // Assert that both events have been sent to the default *producer* cluster ('secondcluster') and not to
        // the default cluster ('default'), the one persisted by the primary outbox bean as well as the one persisted
        // by the test for the unknown cluster 'unknown'.
        assertNumberOfEventsAndIdempotenceIds(testConsumer.testEventsSecondCluster, 2, Set.of(idempotenceIdEvent));
    }

    @SuppressWarnings("SameParameterValue")
    private void assertNumberOfEventsAndIdempotenceIds(List<TestEvent> events, int numEvents, Set<String> eventIdempotenceIds) {
        await().atMost(Duration.ofSeconds(30))
                .until(() -> events.stream().
                        map(event -> event.getIdentity().getIdempotenceId()).
                        filter(eventIdempotenceIds::contains).
                        count() == numEvents);
    }

}
