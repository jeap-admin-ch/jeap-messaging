package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "jeap.messaging.kafka.cluster.default.aws.glue.assume-iam-role-arn=" + JeapGlueSerdeAssumeRoleIT.ROLE_ARN)
class JeapGlueSerdeAssumeRoleIT extends AbstractGlueSerdeTestBase {

    static final String ROLE_ARN = "arn:aws:iam::123456789012:role/testrole";

    @RegisterExtension
    static WireMockExtension stsWiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureStsProperties(DynamicPropertyRegistry registry) {
        registry.add("jeap.messaging.kafka.cluster.default.aws.glue.sts-endpoint", () -> "http://localhost:" + stsWiremock.getPort());
    }

    @Test
    void testMessageSerialization() throws IOException {
        UUID versionId = UUID.randomUUID();
        stubAssumeRoleResponse();
        stubGetSchemaByDefinitionResponse(versionId, "test-topic-TestEvent");
        stubGetSchemaVersionResponse(versionId, TEST_EVENT_AVRO_SCHEMA);

        Serializer<Object> serializer = kafkaAvroSerdeProvider.getValueSerializer();

        AvroMessage testEvent = createTestEvent();
        byte[] serializedRecord = serializer.serialize("test-topic", testEvent);

        assertThat(serializedRecord)
                .isNotNull();
    }

    private static void stubAssumeRoleResponse() throws IOException {
        byte[] response = new ClassPathResource("AssumeRoleResponse.xml").getInputStream().readAllBytes();
        stsWiremock.stubFor(post(urlPathEqualTo("/"))
                .withRequestBody(containing("Action=AssumeRole"))
                .withRequestBody(containing("RoleArn=" + URLEncoder.encode(ROLE_ARN, UTF_8)))
                .withRequestBody(containing("RoleSessionName=testapp"))
                .willReturn(ok(new String(response))));
    }
}