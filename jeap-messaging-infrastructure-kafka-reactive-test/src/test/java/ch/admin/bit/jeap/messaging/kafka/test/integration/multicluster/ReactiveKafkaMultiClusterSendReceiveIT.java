package ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.reactive.JeapSenderReceiverOptionsFactory;
import ch.admin.bit.jeap.messaging.kafka.reactive.test.ReactiveTestMessageSender;
import ch.admin.bit.jeap.messaging.kafka.reactive.test.TestReceiverOptions;
import ch.admin.bit.jeap.messaging.kafka.test.EmbeddedKafkaMultiClusterExtension;
import ch.admin.bit.jeap.messaging.kafka.test.integration.test.TestApp;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
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

import static ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster.ReactiveKafkaMultiClusterSendReceiveIT.*;
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
        "jeap.messaging.kafka.cluster.default.bootstrapServers=localhost:" + (BASE_PORT + PORT_OFFSET),
        "jeap.messaging.kafka.cluster.default.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.default.schemaRegistryUrl=mock://" + MULTI_CLUSTER_REGISTRY_1,
        "jeap.messaging.kafka.cluster.default.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.default.schemaRegistryPassword=unused",
        "jeap.messaging.kafka.cluster.aws.bootstrapServers=localhost:" + (BASE_PORT + PORT_OFFSET + 1),
        "jeap.messaging.kafka.cluster.aws.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.aws.schemaRegistryUrl=mock://" + MULTI_CLUSTER_REGISTRY_2,
        "jeap.messaging.kafka.cluster.aws.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.aws.schemaRegistryPassword=unused"
})
@DirtiesContext
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
class ReactiveKafkaMultiClusterSendReceiveIT {

    static final int BASE_PORT = 12655;
    static final int PORT_OFFSET = 30;

    @RegisterExtension
    static EmbeddedKafkaMultiClusterExtension embeddedKafkaMultiClusterExtension =
            EmbeddedKafkaMultiClusterExtension.withBasePortAndOffset(BASE_PORT, PORT_OFFSET);

    static final String MULTI_CLUSTER_REGISTRY_1 = "multi-cluster-registry-reactive-1";
    static final String MULTI_CLUSTER_REGISTRY_2 = "multi-cluster-registry-reactive-2";

    private static final String AWS_CLUSTER = "aws";
    private static final String TEST_MESSAGE_DEFAULT_CLUSTER = "test event for default cluster";
    private static final String TEST_MESSAGE_AWS_CLUSTER = "test command for aws cluster";

    @Autowired
    private ReactiveKafkaProducerTemplate<AvroMessageKey, AvroMessage> producerTemplate;

    @Autowired
    @Qualifier(AWS_CLUSTER)
    private ReactiveKafkaProducerTemplate<AvroMessageKey, AvroMessage> awsProducerTemplate;

    @Autowired
    private JeapSenderReceiverOptionsFactory senderReceiverOptionsFactory;

    @Autowired
    @Qualifier(AWS_CLUSTER)
    private JeapSenderReceiverOptionsFactory awsSenderReceiverOptionsFactory;


    @Test
    @SneakyThrows
    void testSendAndReceiveOnTwoClusters() {

        // Configure two test message consumers, one for each of the two Kafka clusters
        TestMessageConsumer<AvroMessageKey, JmeCreateDeclarationCommand> commandConsumer = new TestMessageConsumer<>(
            awsSenderReceiverOptionsFactory.createReceiverOptions(JmeCreateDeclarationCommand.TypeRef.DEFAULT_TOPIC));
        TestMessageConsumer<AvroMessageKey, JmeDeclarationCreatedEvent> eventConsumer = new TestMessageConsumer<>(
            TestReceiverOptions.from(senderReceiverOptionsFactory.createReceiverOptions(JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC)));

        // Start consuming messages and wait for a partition to be assigned to the subscription topics
        commandConsumer.startConsuming();
        eventConsumer.startConsuming();

        // Create two test messages, an event to be published to the default cluster and a command to be published to the aws cluster
        JmeDeclarationCreatedEvent event = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .message(TEST_MESSAGE_DEFAULT_CLUSTER)
                .build();
        JmeCreateDeclarationCommand command = JmeCreateDeclarationCommandBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .text(TEST_MESSAGE_AWS_CLUSTER)
                .build();

        // Publish the test messages to the matching clusters
        ReactiveTestMessageSender.sendSyncEnsuringProducerContract(producerTemplate, JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC, event);
        ReactiveTestMessageSender.sendSync(awsProducerTemplate, JmeCreateDeclarationCommand.TypeRef.DEFAULT_TOPIC, command);

        // Check that for each message type its schema has been registered with the schema registry of the cluster that
        // a message of this type has been sent to
        assertThat(MockSchemaRegistry.getClientForScope(MULTI_CLUSTER_REGISTRY_1).getAllSubjects())
                .anyMatch(str -> str.contains(JmeDeclarationCreatedEvent.class.getSimpleName()))
                .noneMatch(str -> str.contains(JmeCreateDeclarationCommand.class.getSimpleName()));
        assertThat(MockSchemaRegistry.getClientForScope(MULTI_CLUSTER_REGISTRY_2).getAllSubjects())
                .anyMatch(str -> str.contains(JmeCreateDeclarationCommand.class.getSimpleName()))
                .noneMatch(str -> str.contains(JmeDeclarationCreatedEvent.class.getSimpleName()));

        // wait for the test messages to have been consumed
        await().atMost(Duration.ofSeconds(60)).until(eventConsumer::hasConsumedMessages);
        await().atMost(Duration.ofSeconds(60)).until(commandConsumer::hasConsumedMessages);

        // check the received messages
        assertThat(eventConsumer.getConsumedMessages()).hasSize(1);
        assertThat(commandConsumer.getConsumedMessages()).hasSize(1);
        assertThat(eventConsumer.getConsumedMessages().get(0).getPayload().getMessage()).isEqualTo(TEST_MESSAGE_DEFAULT_CLUSTER);
        assertThat(commandConsumer.getConsumedMessages().get(0).getPayload().getText()).isEqualTo(TEST_MESSAGE_AWS_CLUSTER);
    }

}
