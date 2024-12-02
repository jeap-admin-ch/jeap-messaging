package ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.reactive.JeapSenderReceiverOptionsFactory;
import ch.admin.bit.jeap.messaging.kafka.test.integration.test.TestApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.test.annotation.DirtiesContext;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApp.class, properties = {
        "spring.application.name=reactive-kafka-beans-per-cluster-test",
        "jeap.messaging.kafka.systemName=test",
        "jeap.messaging.kafka.cluster.default.bootstrapServers=localhost:5000",
        "jeap.messaging.kafka.cluster.default.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.default.schemaRegistryUrl=mock://default",
        "jeap.messaging.kafka.cluster.aws.bootstrapServers=localhost:50001",
        "jeap.messaging.kafka.cluster.aws.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.aws.schemaRegistryUrl=mock://aws",
})
@DirtiesContext
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class ReactiveKafkaBeansPerClusterTest {

    private static final String AWS_CLUSTER = "aws";

    @Autowired
    private ReactiveKafkaProducerTemplate<AvroMessageKey, AvroMessage> producerTemplate;

    @Autowired
    @Qualifier(KafkaProperties.DEFAULT_CLUSTER)
    private ReactiveKafkaProducerTemplate<AvroMessageKey, AvroMessage> defaultProducerTemplate;

    @Autowired
    @Qualifier(AWS_CLUSTER)
    private ReactiveKafkaProducerTemplate<AvroMessageKey, AvroMessage> awsProducerTemplate;


    @Autowired
    private SenderOptions<AvroMessageKey, AvroMessage> senderOptions;

    @Autowired
    @Qualifier(KafkaProperties.DEFAULT_CLUSTER)
    private SenderOptions<AvroMessageKey, AvroMessage> defaultSenderOptions;

    @Autowired
    @Qualifier(AWS_CLUSTER)
    private SenderOptions<AvroMessageKey, AvroMessage> awsSenderOptions;


    @Autowired
    private ReceiverOptions<AvroMessageKey, AvroMessage> receiverOptions;

    @Autowired
    @Qualifier(KafkaProperties.DEFAULT_CLUSTER)
    private ReceiverOptions<AvroMessageKey, AvroMessage> defaultReceiverOptions;

    @Autowired
    @Qualifier(AWS_CLUSTER)
    private ReceiverOptions<AvroMessageKey, AvroMessage> awsReceiverOptions;

    @Autowired
    private JeapSenderReceiverOptionsFactory senderReceiverOptionsFactory;

    @Autowired
    @Qualifier(KafkaProperties.DEFAULT_CLUSTER)
    private JeapSenderReceiverOptionsFactory defaultSenderReceiverOptionsFactory;

    @Autowired
    @Qualifier(AWS_CLUSTER)
    private JeapSenderReceiverOptionsFactory awsSenderReceiverOptionsFactory;


    @Test
    void assertProducerTemplateBeansPresentPerCluster() {
        assertThat(producerTemplate).isNotNull();
        assertThat(defaultProducerTemplate).isNotNull();
        assertThat(awsProducerTemplate).isNotNull();

        assertThat(producerTemplate)
                .describedAs("Two different producer template beans expected, one for each cluster configuration.")
                .isNotSameAs(awsProducerTemplate);
        assertThat(producerTemplate)
                .describedAs("Unqualified producer bean is the same as the default cluster bean.")
                .isSameAs(defaultProducerTemplate);
    }

    @Test
    void assertSenderOptionsBeansPresentPerCluster() {
        assertThat(senderOptions).isNotNull();
        assertThat(defaultSenderOptions).isNotNull();
        assertThat(awsSenderOptions).isNotNull();

        assertThat(senderOptions)
                .describedAs("Two different sender options beans expected, one for each cluster configuration.")
                .isNotSameAs(awsSenderOptions);
        assertThat(senderOptions)
                .describedAs("Unqualified sender options bean is the same as the default cluster bean.")
                .isSameAs(defaultSenderOptions);
    }

    @Test
    void assertReceiverOptionsBeansPresentPerCluster() {
        assertThat(receiverOptions).isNotNull();
        assertThat(defaultReceiverOptions).isNotNull();
        assertThat(awsReceiverOptions).isNotNull();

        assertThat(receiverOptions)
                .describedAs("Two different receiver options beans expected, one for each cluster configuration.")
                .isNotSameAs(awsReceiverOptions);
        assertThat(receiverOptions)
                .describedAs("Unqualified receiver options bean is the same as the default cluster bean.")
                .isSameAs(defaultReceiverOptions);
    }

    @Test
    void assertJeapSenderReceiverOptionsFactoryBeanPresentPerCluster() {
        assertThat(senderReceiverOptionsFactory).isNotNull();
        assertThat(defaultSenderReceiverOptionsFactory).isNotNull();
        assertThat(awsSenderReceiverOptionsFactory).isNotNull();

        assertThat(senderReceiverOptionsFactory)
                .describedAs("Two different sender receiver options factory beans expected, one for each cluster configuration.")
                .isNotSameAs(awsSenderReceiverOptionsFactory);
        assertThat(senderReceiverOptionsFactory)
                .describedAs("Unqualified sender receiver options factory bean is the same as the default cluster bean.")
                .isSameAs(defaultSenderReceiverOptionsFactory);

        assertThat(senderReceiverOptionsFactory.createSenderOptions()).isEqualTo(senderOptions);
        assertThat(senderReceiverOptionsFactory.createReceiverOptions()).isEqualTo(receiverOptions);

        final String[] topics = {"topic-a", "topic-b"};
        assertThat(senderReceiverOptionsFactory.createReceiverOptions(topics).subscriptionTopics()).containsOnly(topics);
    }

}
