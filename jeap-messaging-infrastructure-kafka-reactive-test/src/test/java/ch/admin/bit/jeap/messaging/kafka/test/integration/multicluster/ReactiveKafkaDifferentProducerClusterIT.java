package ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.reactive.JeapSenderReceiverOptionsFactory;
import ch.admin.bit.jeap.messaging.kafka.reactive.test.ReactiveTestMessageSender;
import ch.admin.bit.jeap.messaging.kafka.reactive.test.TestReceiverOptions;
import ch.admin.bit.jeap.messaging.kafka.test.EmbeddedKafkaMultiClusterExtension;
import ch.admin.bit.jeap.messaging.kafka.test.integration.test.TestApp;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.UUID;

import static ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster.ReactiveKafkaDifferentProducerClusterIT.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(classes = TestApp.class, properties = {
        "spring.application.name=jme-messaging-receiverpublisher-service",
        "spring.kafka.template.default-topic=default-test-topic",
        "jeap.messaging.kafka.embedded=false",
        "jeap.messaging.kafka.systemName=test",
        "jeap.messaging.kafka.errorTopicName=errorTopic",
        "jeap.messaging.kafka.message-type-encryption-disabled=true",
        "jeap.messaging.kafka.cluster.consumer.bootstrapServers=localhost:" + (BASE_PORT + PORT_OFFSET),
        "jeap.messaging.kafka.cluster.consumer.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.consumer.schemaRegistryUrl=mock://" + MULTI_CLUSTER_REGISTRY_CONSUMER,
        "jeap.messaging.kafka.cluster.consumer.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.consumer.schemaRegistryPassword=unused",
        "jeap.messaging.kafka.cluster.producer.default-producer-cluster-override=true",
        "jeap.messaging.kafka.cluster.producer.bootstrapServers=localhost:" + (BASE_PORT + PORT_OFFSET + 1),
        "jeap.messaging.kafka.cluster.producer.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.producer.schemaRegistryUrl=mock://" + MULTI_CLUSTER_REGISTRY_PRODUCER,
        "jeap.messaging.kafka.cluster.producer.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.producer.schemaRegistryPassword=unused"
})
@DirtiesContext
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
class ReactiveKafkaDifferentProducerClusterIT {

    static final int BASE_PORT = 12655;
    static final int PORT_OFFSET = 35;

    @RegisterExtension
    static EmbeddedKafkaMultiClusterExtension embeddedKafkaMultiClusterExtension =
            EmbeddedKafkaMultiClusterExtension.withBasePortAndOffset(BASE_PORT, PORT_OFFSET);

    static final String MULTI_CLUSTER_REGISTRY_PRODUCER = "multi-cluster-registry-reactive-producer";
    static final String MULTI_CLUSTER_REGISTRY_CONSUMER = "multi-cluster-registry-reactive-consumer";

    @Autowired
    private ReactiveKafkaProducerTemplate<AvroMessageKey, AvroMessage> producerTemplate;

    @Autowired
    @Qualifier("producer")
    private JeapSenderReceiverOptionsFactory producerClusterOptionsFactory;

    @Test
    @SneakyThrows
    void testProduceToProducerOverrideCluster() {

        // Configure a test message consumer for the producer cluster
        TestMessageConsumer<AvroMessageKey, JmeDeclarationCreatedEvent> eventConsumer = new TestMessageConsumer<>(
                TestReceiverOptions.from(producerClusterOptionsFactory.createReceiverOptions(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC)));
        eventConsumer.startConsuming();

        // Create a test message
        JmeDeclarationCreatedEvent event = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .message("message")
                .build();

        // Publish the test message
        ReactiveTestMessageSender.sendSync(producerTemplate, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event);

        // Check that the schema has been registered with the schema registry of the producer cluster
        assertThat(MockSchemaRegistry.getClientForScope(MULTI_CLUSTER_REGISTRY_PRODUCER).getAllSubjects())
                .anyMatch(str -> str.contains(JmeDeclarationCreatedEvent.class.getSimpleName()));
        assertThat(MockSchemaRegistry.getClientForScope(MULTI_CLUSTER_REGISTRY_CONSUMER).getAllSubjects())
                .isEmpty();

        // wait for the test message to have been consumed
        await()
                .atMost(Duration.ofSeconds(30))
                .until(eventConsumer::hasConsumedMessages);

        // check the received message
        assertThat(eventConsumer.getConsumedMessages())
                .hasSize(1);
        assertThat(eventConsumer.getConsumedMessages().get(0).getIdentity().getIdempotenceId())
                .isEqualTo(event.getIdentity().getIdempotenceId());
    }
}
