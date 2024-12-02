package ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.test.EmbeddedKafkaMultiClusterExtension;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.function.Predicate;

import static ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster.DifferentConsumerProducerClusterTestConfiguration.TestConsumer;
import static ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster.KafkaDifferentConsumerProducerClusterIT.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = TestConfig.class, properties = {
        "spring.application.name=jme-messaging-receiverpublisher-service",
        "jeap.messaging.kafka.embedded=false",
        "jeap.messaging.kafka.systemName=test",
        "jeap.messaging.kafka.errorTopicName=errorTopic",
        "jeap.messaging.kafka.message-type-encryption-disabled=true",
        "jeap.messaging.kafka.cluster.producer.default-producer-cluster-override=true",
        "jeap.messaging.kafka.cluster.producer.bootstrapServers=localhost:" + (EmbeddedKafkaMultiClusterExtension.BASE_PORT + PORT_OFFSET),
        "jeap.messaging.kafka.cluster.producer.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.producer.schemaRegistryUrl=mock://" + MULTI_CLUSTER_REGISTRY_PRODUCER,
        "jeap.messaging.kafka.cluster.producer.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.producer.schemaRegistryPassword=unused",
        "jeap.messaging.kafka.cluster.consumer.default-cluster=true", // Default cluster is the consumer's cluster
        "jeap.messaging.kafka.cluster.consumer.bootstrapServers=localhost:" + (EmbeddedKafkaMultiClusterExtension.BASE_PORT + PORT_OFFSET + 1),
        "jeap.messaging.kafka.cluster.consumer.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.consumer.schemaRegistryUrl=mock://" + MULTI_CLUSTER_REGISTRY_CONSUMER,
        "jeap.messaging.kafka.cluster.consumer.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.consumer.schemaRegistryPassword=unused"
})
@Import(DifferentConsumerProducerClusterTestConfiguration.class)
class KafkaDifferentConsumerProducerClusterIT extends KafkaMultiClusterIntegrationTestBase {

    static final int PORT_OFFSET = 20;

    @RegisterExtension
    static EmbeddedKafkaMultiClusterExtension embeddedKafkaMultiClusterExtension =
            EmbeddedKafkaMultiClusterExtension.withPortOffset(PORT_OFFSET);

    static final String MULTI_CLUSTER_REGISTRY_PRODUCER = "multi-cluster-registry-producer";
    static final String MULTI_CLUSTER_REGISTRY_CONSUMER = "multi-cluster-registry-consumer";

    @Autowired
    private TestConsumer testConsumer;

    @Autowired
    protected KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplateDefaultProducerCluster;

    @Qualifier("consumer")
    @Autowired
    protected KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplateExplicitConsumerCluster;

    @Test
    void publishEventToDefaultProducerCluster() throws Exception {
        JmeDeclarationCreatedEvent event = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .message("sendEventWithContract")
                .build();
        Predicate<JmeDeclarationCreatedEvent> matchesEvent = e ->
                e.getIdentity().getIdempotenceId().equals(event.getIdentity().getIdempotenceId());

        // Publish message to default producer cluster
        sendSync(kafkaTemplateDefaultProducerCluster, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event);

        // Wait until message has been received
        await().until(() -> testConsumer.getProducerClusterEvents().stream().anyMatch(matchesEvent));

        // Assert that event has been received as expected on producer cluster
        assertThat(testConsumer.getProducerClusterEvents().stream().filter(matchesEvent).toList())
                .filteredOn(matchesEvent)
                .hasSize(1);
        assertThat(testConsumer.getConsumerClusterEvents().stream().toList())
                .noneMatch(matchesEvent);
    }

    @Test
    void publishEventExplicitlyToConsumerCluster() {
        JmeDeclarationCreatedEvent event = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .message("sendEventWithContract")
                .build();
        Predicate<JmeDeclarationCreatedEvent> matchesEvent = e ->
                e.getIdentity().getIdempotenceId().equals(event.getIdentity().getIdempotenceId());

        // Publish message explicitly to consumer cluster
        sendSync(kafkaTemplateExplicitConsumerCluster, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event);

        // Wait until message has been received
        await().until(() -> testConsumer.getConsumerClusterEvents().stream().anyMatch(matchesEvent));

        // Assert that event has been received as expected on consumer cluster
        assertThat(testConsumer.getConsumerClusterEvents().stream().filter(matchesEvent).toList())
                .filteredOn(matchesEvent)
                .hasSize(1);
        assertThat(testConsumer.getProducerClusterEvents().stream().toList())
                .noneMatch(matchesEvent);
    }


    @Test
    void assertMessageProcessingFailedEventOnProducerCluster() {
        JmeDeclarationCreatedEvent consumerClusterMessage = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .message("error-on-default-cluster")
                .build();
        Predicate<MessageProcessingFailedEvent> matchesFailedEvent = e ->
                e.getPayload().getFailedMessageMetadata().getIdempotenceId().equals(consumerClusterMessage.getIdentity().getIdempotenceId());

        // Publish original message to consumer cluster, provoking an exception in the listener
        sendSync(kafkaTemplateExplicitConsumerCluster, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, consumerClusterMessage);

        // Wait until MessageProcessingFailedEvent has been received on producer cluster
        await().until(() -> testConsumer.getProducerClusterFailedEvents().stream().anyMatch(matchesFailedEvent));

        // Assert that MessageProcessingFailedEvent has been received as expected on producer cluster
        assertThat(testConsumer.getProducerClusterFailedEvents().stream().filter(matchesFailedEvent).toList())
                .hasSize(1);
        assertThat(testConsumer.getConsumerClusterFailedEvents().stream().filter(matchesFailedEvent).toList())
                .isEmpty();
    }
}
