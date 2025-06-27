package ch.admin.bit.jeap.messaging.kafka.test.integration.glue;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureConfiguration;
import ch.admin.bit.jeap.messaging.kafka.signature.subscriber.CertificateAndSignatureVerifier;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeSimpleTestEventBuilder;
import ch.admin.bit.jme.test.BeanReferenceMessageKey;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(classes = {TestGlueConfig.class, SignatureConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=jme-messaging-receiverpublisher-service",
                "management.endpoints.web.exposure.include=*",
                "spring.kafka.template.default-topic=default-test-topic",
                "jeap.messaging.kafka.embedded=false",
                "jeap.messaging.kafka.systemName=test",
                "jeap.messaging.kafka.errorTopicName=errorTopic",
                "jeap.messaging.kafka.message-type-encryption-disabled=true",
                "jeap.messaging.kafka.cluster.aws.aws.glue.registryName=testregistry",
                "jeap.messaging.kafka.cluster.aws.aws.glue.region=eu-test-1",
                "jeap.messaging.kafka.cluster.aws.bootstrapServers=${spring.embedded.kafka.brokers}",
                "jeap.messaging.kafka.cluster.aws.securityProtocol=PLAINTEXT",
                "jeap.messaging.kafka.exposeMessageKeyToConsumer=false"
        })
@AutoConfigureObservability
@Import({KafkaSerdeGlueIT.TestConsumerConfig.class})
@ActiveProfiles({"test-signing-publisher", "test-signing-subscriber"})
class KafkaSerdeSigningSendAndReceiveNoKeyExposureGlueIT extends KafkaGlueIntegrationTestBase {

    @Qualifier("aws")
    @Autowired
    protected KafkaTemplate<AvroMessageKey, AvroMessage> awsKafkaTemplate;

    @Qualifier("aws")
    @Autowired
    @SuppressWarnings("unused")
    protected KafkaAdmin awsKafkaAdmin;

    @Autowired
    protected KafkaProperties kafkaProperties;

    @MockitoSpyBean
    private CertificateAndSignatureVerifier certificateAndSignatureVerifier;

    static final String JME_SIMPLE_TEST_EVENT_AVRO_SCHEMA = JmeSimpleTestEvent.SCHEMA$.toString().replace("\"", "\\\"");
    static final String JME_BEAN_REFERENCE_MESSAGE_KEY_AVRO_SCHEMA = BeanReferenceMessageKey.SCHEMA$.toString().replace("\"", "\\\"");

    @Test
    void testSignHeaders_sendWithKey_noKeyExposure() {
        UUID simpleTestEventVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(simpleTestEventVersionId, "some-other-topic-JmeSimpleTestEvent");
        stubGetSchemaVersionResponse(simpleTestEventVersionId, JME_SIMPLE_TEST_EVENT_AVRO_SCHEMA);

        UUID beanReferenceMessageKeyVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(beanReferenceMessageKeyVersionId, "some-other-topic-BeanReferenceMessageKey-key");
        stubGetSchemaVersionResponse(beanReferenceMessageKeyVersionId, JME_BEAN_REFERENCE_MESSAGE_KEY_AVRO_SCHEMA);

        BeanReferenceMessageKey messageKey = new BeanReferenceMessageKey("myKey", "myNamespace", UUID.randomUUID().toString());
        JmeSimpleTestEvent message = JmeSimpleTestEventBuilder.create()
                .idempotenceId("idempotenceId")
                .serviceName("jme-messaging-receiverpublisher-service")
                .message("message")
                .build();

        List<Object> authenticityCheckResults = new ArrayList<>();
        List<Object> authenticityKeyCheckResults = new ArrayList<>();

        // For value
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod(); // Call real method
            authenticityCheckResults.add(result);
            return result;
        }).when(certificateAndSignatureVerifier).verifyValueSignature(any(), any(), any(), any());
        // For key
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod(); // Call real method
            authenticityKeyCheckResults.add(result);
            return result;
        }).when(certificateAndSignatureVerifier).verifyKeySignature(any(), any(), any());

        sendSync(awsKafkaTemplate, JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer.OTHER_TOPIC_NAME, messageKey, message);

        await().until(() -> testConsumer.getSimpleTestV2EventsFromRecord().size() == 1);

        assertEquals(1, authenticityCheckResults.size());
        authenticityCheckResults.forEach(value -> assertTrue((Boolean) value));
        assertEquals(0, authenticityKeyCheckResults.size());
    }
}
