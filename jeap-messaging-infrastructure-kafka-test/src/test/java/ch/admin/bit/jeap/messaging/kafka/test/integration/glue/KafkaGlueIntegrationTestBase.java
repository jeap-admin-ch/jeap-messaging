package ch.admin.bit.jeap.messaging.kafka.test.integration.glue;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaTestConstants;
import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import ch.admin.bit.jeap.messaging.kafka.test.TestMessageSender;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.BeanReferenceMessageKeyV2;
import ch.admin.bit.jme.test.JmeSimpleTestV2Event;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.glue.model.SchemaVersionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@EmbeddedKafka(controlledShutdown = true, partitions = 1)
abstract public class KafkaGlueIntegrationTestBase {
    protected static final int TEST_TIMEOUT = 10000;

    public static final String TOPIC_NAME = "some-topic";

    public static final String OTHER_TOPIC_NAME = "some-other-topic";

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @BeforeEach
    void waitForKafkaListener() {
        registry.getListenerContainers()
                .forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
        testConsumer.reset();
    }

    protected void sendSync(KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate, String topic, AvroMessage message) {
        TestMessageSender.sendSync(kafkaTemplate, topic, message);
    }

    protected void sendSync(KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate, String topic, AvroMessageKey messageKey, AvroMessage message) {
        TestMessageSender.sendSync(kafkaTemplate, topic, messageKey, message);
    }

    @Autowired
    TestConsumer testConsumer;

    @TestConfiguration
    static class TestConsumerConfig {
        @Bean
        TestConsumer testConsumer() {
            return new TestConsumer();
        }

        @Bean
        AwsCredentialsProvider awsCredentialsProvider() {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
        }

    }

    @Getter
    static class TestConsumer {
        private final List<JmeCreateDeclarationCommand> createDeclarationCommands = new ArrayList<>();
        private final List<JmeDeclarationCreatedEvent> declarationCreatedEvents = new ArrayList<>();
        private final List<JmeSimpleTestV2Event> simpleTestV2Events = new ArrayList<>();
        private final List<BeanReferenceMessageKeyV2> beanReferenceMessageKeyV2Keys = new ArrayList<>();
        private final List<JmeSimpleTestV2Event> simpleTestV2EventsFromRecord = new ArrayList<>();

        void reset() {
            createDeclarationCommands.clear();
            declarationCreatedEvents.clear();
            simpleTestV2Events.clear();
            beanReferenceMessageKeyV2Keys.clear();
            simpleTestV2EventsFromRecord.clear();
        }

        @TestKafkaListener(topics = JmeCreateDeclarationCommand.TypeRef.DEFAULT_TOPIC, groupId = "test-aws", properties = {
                "specific.avro.value.type=ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand"
        })
        public void consume(JmeCreateDeclarationCommand command) {
            createDeclarationCommands.add(command);
        }

        @TestKafkaListener(topics = JmeDeclarationCreatedEvent.TypeRef.DEFAULT_TOPIC)
        public void consume(final JmeDeclarationCreatedEvent event) {
            declarationCreatedEvents.add(event);
        }

        @TestKafkaListener(topics = TOPIC_NAME,
                properties = {
                        "specific.avro.value.type=ch.admin.bit.jme.test.JmeSimpleTestV2Event",
                        KafkaTestConstants.TEST_CONSUMER_DISABLE_CONTRACT_CHECK_PROPERTY
                })
        public void consume(JmeSimpleTestV2Event event) {
            simpleTestV2Events.add(event);
        }

        @TestKafkaListener(topics = OTHER_TOPIC_NAME,
                properties = {
                        "specific.avro.key.type=ch.admin.bit.jme.test.BeanReferenceMessageKeyV2",
                        "specific.avro.value.type=ch.admin.bit.jme.test.JmeSimpleTestV2Event",
                        KafkaTestConstants.TEST_CONSUMER_DISABLE_CONTRACT_CHECK_PROPERTY
                }
        )
        public void consume(ConsumerRecord consumerRecord) {
            if (consumerRecord.key() instanceof BeanReferenceMessageKeyV2) {
                BeanReferenceMessageKeyV2 key = (BeanReferenceMessageKeyV2) consumerRecord.key();
                beanReferenceMessageKeyV2Keys.add(key);
            }
            JmeSimpleTestV2Event value = (JmeSimpleTestV2Event) consumerRecord.value();
            simpleTestV2EventsFromRecord.add(value);
        }


    }

    @RegisterExtension
    static WireMockExtension glueWiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureGlueProperties(DynamicPropertyRegistry registry) {
        registry.add("jeap.messaging.kafka.cluster.aws.aws.glue.endpoint", () -> "http://localhost:" + glueWiremock.getPort());
    }

    static void stubGetSchemaVersionResponse(UUID versionId, String avroSchema) {
        String getSchemaVersionResponse = """
                {
                   "DataFormat": "AVRO",
                   "SchemaDefinition": "%s",
                   "SchemaVersionId": "%s",
                   "SchemaArn": "arn:aws:glue:eu-test-1:123456789012:schema/testregistry/test",
                   "Status": "%s",
                   "VersionNumber": 1
                }"
                """.formatted(avroSchema, versionId, SchemaVersionStatus.AVAILABLE);
        glueWiremock.stubFor(post("/")
                .withHeader("X-Amz-Target", equalTo("AWSGlue.GetSchemaVersion"))
                .withRequestBody(containing(versionId.toString()))
                .willReturn(ok(getSchemaVersionResponse)));
    }

    static void stubGetSchemaByDefinitionResponse(UUID versionId, String subjectName) {
        String getSchemaByDefinitionResponse = """
                {
                "SchemaVersionId": "%s",
                "Status": "%s"
                }
                """.formatted(versionId, SchemaVersionStatus.AVAILABLE);
        glueWiremock.stubFor(post("/")
                .withHeader("X-Amz-Target", equalTo("AWSGlue.GetSchemaByDefinition"))
                .withRequestBody(containing("\"SchemaName\":\"" + subjectName + '"'))
                .willReturn(ok(getSchemaByDefinitionResponse)));
    }
}
