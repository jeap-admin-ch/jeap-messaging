package ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.test.EmbeddedKafkaMultiClusterExtension;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeCreateDeclarationCommandBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Predicate;

import static ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster.KafkaMultiClusterIT.*;
import static ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster.MultiClusterTestConfiguration.TestConsumer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = TestConfig.class, properties = {
        "spring.application.name=jme-messaging-receiverpublisher-service",
        "spring.kafka.template.default-topic=default-test-topic",
        "jeap.messaging.kafka.embedded=false",
        "jeap.messaging.kafka.systemName=test",
        "jeap.messaging.kafka.errorTopicName=errorTopic",
        "jeap.messaging.kafka.message-type-encryption-disabled=true",
        "jeap.messaging.kafka.cluster.default.bootstrapServers=localhost:" + (EmbeddedKafkaMultiClusterExtension.BASE_PORT + PORT_OFFSET),
        "jeap.messaging.kafka.cluster.default.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.default.schemaRegistryUrl=mock://" + MULTI_CLUSTER_REGISTRY_1,
        "jeap.messaging.kafka.cluster.default.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.default.schemaRegistryPassword=unused",
        "jeap.messaging.kafka.cluster.aws.bootstrapServers=localhost:" + (EmbeddedKafkaMultiClusterExtension.BASE_PORT + PORT_OFFSET + 1),
        "jeap.messaging.kafka.cluster.aws.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.aws.schemaRegistryUrl=mock://" + MULTI_CLUSTER_REGISTRY_2,
        "jeap.messaging.kafka.cluster.aws.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.aws.schemaRegistryPassword=unused"
})
@Import(MultiClusterTestConfiguration.class)
class KafkaMultiClusterIT extends KafkaMultiClusterIntegrationTestBase {

    static final int PORT_OFFSET = 10;

    @RegisterExtension
    static EmbeddedKafkaMultiClusterExtension embeddedKafkaMultiClusterExtension =
            EmbeddedKafkaMultiClusterExtension.withPortOffset(PORT_OFFSET);

    static final String MULTI_CLUSTER_REGISTRY_1 = "multi-cluster-registry-1";
    static final String MULTI_CLUSTER_REGISTRY_2 = "multi-cluster-registry-2";

    @Autowired
    private TestConsumer testConsumer;

    @Autowired
    protected KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;

    @Qualifier(KafkaProperties.DEFAULT_CLUSTER)
    @Autowired
    protected KafkaTemplate<AvroMessageKey, AvroMessage> defaultKafkaTemplate;

    @Qualifier("aws")
    @Autowired
    protected KafkaTemplate<AvroMessageKey, AvroMessage> awsKafkaTemplate;

    @Autowired
    protected KafkaAdmin kafkaAdmin;

    @Qualifier("aws")
    @Autowired
    protected KafkaAdmin awsKafkaAdmin;

    @Test
    void publishReceiveEventUsingDifferentClusters() throws InterruptedException, RestClientException, IOException {
        assertThat(kafkaTemplate)
                .describedAs("Two different template beans expected, one for each cluster configuration")
                .isNotSameAs(awsKafkaTemplate)
                .isSameAs(defaultKafkaTemplate);
        assertThat(kafkaTemplate.getDefaultTopic())
                .isEqualTo("default-test-topic");

        JmeDeclarationCreatedEvent event = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .message("sendEventWithContract")
                .build();
        Predicate<JmeDeclarationCreatedEvent> matchesDefaultClusterEvent = e ->
                e.getIdentity().getIdempotenceId().equals(event.getIdentity().getIdempotenceId());
        JmeCreateDeclarationCommand awsMessage = JmeCreateDeclarationCommandBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .text("text")
                .build();
        Predicate<JmeCreateDeclarationCommand> matchesAwsCommand = e ->
                e.getIdentity().getIdempotenceId().equals(awsMessage.getIdentity().getIdempotenceId());

        // Publish messages on each cluster
        sendSync(kafkaTemplate, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event);
        sendSync(awsKafkaTemplate, JmeCreateDeclarationCommand.TypeRef.DEFAULT_TOPIC, awsMessage);

        // Wait until messages have been received by both clusters/brokers
        await().until(() -> testConsumer.getDefaultClusterEvents().stream().anyMatch(matchesDefaultClusterEvent));
        await().until(() -> testConsumer.getAwsClusterCommands().stream().anyMatch(matchesAwsCommand));

        // Assert that events have been consumed as expected on both clusters/brokers
        assertThat(testConsumer.getDefaultClusterEvents().stream().filter(matchesDefaultClusterEvent).toList())
                .filteredOn(matchesDefaultClusterEvent)
                .hasSize(1);
        assertThat(testConsumer.getAwsClusterCommands().stream().toList())
                .filteredOn(matchesAwsCommand)
                .hasSize(1);

        assertThat(MockSchemaRegistry.getClientForScope(MULTI_CLUSTER_REGISTRY_1).getAllSubjects())
                .anyMatch(str -> str.contains(JmeDeclarationCreatedEvent.class.getSimpleName()))
                .noneMatch(str -> str.contains(JmeCreateDeclarationCommand.class.getSimpleName()));
        assertThat(MockSchemaRegistry.getClientForScope(MULTI_CLUSTER_REGISTRY_2).getAllSubjects())
                .anyMatch(str -> str.contains(JmeCreateDeclarationCommand.class.getSimpleName()))
                .noneMatch(str -> str.contains(JmeDeclarationCreatedEvent.class.getSimpleName()));

        assertThat(kafkaTemplate.getKafkaAdmin())
                .isSameAs(kafkaAdmin);
        assertThat(awsKafkaTemplate.getKafkaAdmin())
                .isSameAs(awsKafkaAdmin);
    }

    @Test
    void assertMessageProcessingFailedEventOnConsumerCluster() {
        JmeDeclarationCreatedEvent defaultClusterMessage = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .message("error-on-default-cluster")
                .build();
        Predicate<MessageProcessingFailedEvent> matchesDefaultClusterEvent = e ->
                e.getPayload().getFailedMessageMetadata().getIdempotenceId().equals(defaultClusterMessage.getIdentity().getIdempotenceId());
        JmeCreateDeclarationCommand awsMessage = JmeCreateDeclarationCommandBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .text("error-on-aws")
                .build();
        Predicate<MessageProcessingFailedEvent> matchesAwsEvent = e ->
                e.getPayload().getFailedMessageMetadata().getIdempotenceId().equals(awsMessage.getIdentity().getIdempotenceId());

        // Publish original message to both clusters/brokers, provoking an exception in the listener
        sendSync(kafkaTemplate, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, defaultClusterMessage);
        sendSync(awsKafkaTemplate, JmeCreateDeclarationCommand.TypeRef.DEFAULT_TOPIC, awsMessage);

        // Wait until MessageProcessingFailedEvents have been received by both clusters/brokers
        await().until(() -> testConsumer.getDefaultClusterFailedEvents().stream().anyMatch(matchesDefaultClusterEvent));
        await().until(() -> testConsumer.getAwsClusterFailedEvents().stream().anyMatch(matchesAwsEvent));

        // Assert that MessageProcessingFailedEvents have been consumed as expected on both clusters/brokers
        assertThat(testConsumer.getDefaultClusterFailedEvents().stream().filter(matchesDefaultClusterEvent).toList())
                .noneMatch(matchesAwsEvent)
                .filteredOn(matchesDefaultClusterEvent)
                .hasSize(1);
        assertThat(testConsumer.getAwsClusterFailedEvents().stream().filter(matchesAwsEvent).toList())
                .noneMatch(matchesDefaultClusterEvent)
                .filteredOn(matchesAwsEvent)
                .hasSize(1);
    }
}
