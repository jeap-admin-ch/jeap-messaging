package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventIdentity;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventPublisher;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventType;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.test.TestApp;
import ch.admin.bit.jeap.messaging.test.glue.avro.TestEvent;
import ch.admin.bit.jeap.messaging.test.glue.avro.TestMessageKey;
import ch.admin.bit.jeap.messaging.test.glue.avro.TestReferences;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.glue.model.SchemaVersionStatus;

import java.time.Instant;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;


@SpringBootTest(
        classes = TestApp.class,
        properties = {
                "spring.application.name=testapp",
                "jeap.messaging.kafka.cluster.default.aws.glue.registryName=testregistry",
                "jeap.messaging.kafka.cluster.default.aws.glue.region=eu-test-1",
                "jeap.messaging.kafka.systemName=test",
                "jeap.messaging.kafka.serviceName=test-service",
                "jeap.messaging.kafka.expose-message-key-to-consumer=true"
        }
)
@Import(AbstractGlueSerdeTestBase.TestConfig.class)
public abstract class AbstractGlueSerdeTestBase {

    static final String TEST_EVENT_AVRO_SCHEMA = TestEvent.SCHEMA$.toString().replace("\"", "\\\"");
    static final String TEST_KEY_AVRO_SCHEMA = TestMessageKey.SCHEMA$.toString().replace("\"", "\\\"");

    static class TestConfig {
        @Bean
        AwsCredentialsProvider awsCredentialsProvider() {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
        }
    }

    @RegisterExtension
    static WireMockExtension glueWiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureGlueProperties(DynamicPropertyRegistry registry) {
        registry.add("jeap.messaging.kafka.cluster.default.aws.glue.endpoint", () -> "http://localhost:" + glueWiremock.getPort());
    }

    @Autowired
    KafkaAvroSerdeProvider kafkaAvroSerdeProvider;

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


    AvroMessageKey createTestMessageKey() {
        return TestMessageKey.newBuilder().setValue("test-key").build();
    }

    static AvroMessage createTestEvent() {
        return TestEvent.newBuilder()
                .setIdentityBuilder(AvroDomainEventIdentity.newBuilder()
                        .setEventId("id")
                        .setCreated(Instant.now())
                        .setIdempotenceId("id"))
                .setReferencesBuilder(TestReferences.newBuilder())
                .setTypeBuilder(AvroDomainEventType.newBuilder()
                        .setName("TestEvent")
                        .setVersion("1.0.0"))
                .setPublisherBuilder(AvroDomainEventPublisher.newBuilder()
                        .setService("service")
                        .setSystem("system"))
                .setDomainEventVersion("1.1.0")
                .build();
    }
}
