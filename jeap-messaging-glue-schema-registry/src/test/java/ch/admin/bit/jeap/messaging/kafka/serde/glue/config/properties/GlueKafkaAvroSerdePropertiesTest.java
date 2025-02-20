package ch.admin.bit.jeap.messaging.kafka.serde.glue.config.properties;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.GlueProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.EmptyKeyDeserializer;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureAuthenticityService;
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlueKafkaAvroSerdePropertiesTest {

    private static final String TEST_REGISTRY = "test-registry";
    private static final String MY_SERVICE = "my-service";
    private static final String MY_SYSTEM = "my-system";
    public static final String REGION = "eu-test-1";

    @Test
    void avroSerializerProperties() {
        KafkaProperties kafkaProps = mock(KafkaProperties.class);
        when(kafkaProps.getServiceName()).thenReturn(MY_SERVICE);
        when(kafkaProps.getSystemName()).thenReturn(MY_SYSTEM);
        GlueProperties configProps = new GlueProperties();
        configProps.setRegion(REGION);
        configProps.setRegistryName(TEST_REGISTRY);

        GlueKafkaAvroSerdeProperties props = new GlueKafkaAvroSerdeProperties(kafkaProps, configProps, null, null, null);

        Map<String, Object> serializerProperties = props.avroSerializerProperties(KafkaProperties.DEFAULT_CLUSTER);
        assertThat(serializerProperties)
                .containsEntry(AWSSchemaRegistryConstants.AWS_REGION, REGION)
                .containsEntry(AWSSchemaRegistryConstants.METADATA, Map.of(
                        "registered-by-service", MY_SERVICE,
                        "registered-by-system", MY_SYSTEM))
                .containsEntry(AWSSchemaRegistryConstants.REGISTRY_NAME, "test-registry")
                .containsEntry(AWSSchemaRegistryConstants.DESCRIPTION, "test-registry my-service");
    }

    @Test
    void avroSerializerProperties_missingProperty() {
        KafkaProperties kafkaProps = mock(KafkaProperties.class);
        GlueProperties configProps = new GlueProperties();
        GlueKafkaAvroSerdeProperties props = new GlueKafkaAvroSerdeProperties(kafkaProps, configProps, null, null, null);

        assertThatThrownBy(() -> props.avroSerializerProperties(KafkaProperties.DEFAULT_CLUSTER))
                .hasMessageContaining("jeap.messaging.kafka.systemName");

    }

    @Test
    void avroDeserializerProperties() {
        KafkaProperties kafkaProps = mock(KafkaProperties.class);
        when(kafkaProps.getServiceName()).thenReturn(MY_SERVICE);
        when(kafkaProps.getSystemName()).thenReturn(MY_SYSTEM);
        GlueProperties configProps = new GlueProperties();
        configProps.setRegion("eu-test-1");
        configProps.setRegistryName(TEST_REGISTRY);

        AwsCredentialsProvider awsCredentialsProvider = mock(AwsCredentialsProvider.class);
        GlueKafkaAvroSerdeProperties props = new GlueKafkaAvroSerdeProperties(kafkaProps, configProps, awsCredentialsProvider, null, null);

        Map<String, Object> deserializerProperties = props.avroDeserializerProperties(KafkaProperties.DEFAULT_CLUSTER);
        assertThat(deserializerProperties)
                .containsEntry(AWSSchemaRegistryConstants.AWS_REGION, REGION)
                .containsEntry(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, EmptyKeyDeserializer.class)
                .containsEntry(GlueKafkaAvroSerdeProperties.GLUE_AWS_CREDENTIALS_PROVIDER, awsCredentialsProvider);

    }

    @Test
    void avroDeserializerProperties_withSignatureAuthenticityService() {
        KafkaProperties kafkaProps = mock(KafkaProperties.class);
        when(kafkaProps.getServiceName()).thenReturn(MY_SERVICE);
        when(kafkaProps.getSystemName()).thenReturn(MY_SYSTEM);
        GlueProperties configProps = new GlueProperties();
        configProps.setRegion("eu-test-1");
        configProps.setRegistryName(TEST_REGISTRY);

        AwsCredentialsProvider awsCredentialsProvider = mock(AwsCredentialsProvider.class);
        SignatureAuthenticityService signatureAuthenticityService = mock(SignatureAuthenticityService.class);
        GlueKafkaAvroSerdeProperties props = new GlueKafkaAvroSerdeProperties(kafkaProps, configProps, awsCredentialsProvider, null, signatureAuthenticityService);

        Map<String, Object> deserializerProperties = props.avroDeserializerProperties(KafkaProperties.DEFAULT_CLUSTER);
        assertThat(deserializerProperties)
                .containsEntry(AWSSchemaRegistryConstants.AWS_REGION, REGION)
                .containsEntry(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, EmptyKeyDeserializer.class)
                .containsEntry(GlueKafkaAvroSerdeProperties.GLUE_AWS_CREDENTIALS_PROVIDER, awsCredentialsProvider)
                .containsEntry(GlueKafkaAvroSerdeProperties.JEAP_SIGNATURE_AUTHENTICITY_SERVICE, signatureAuthenticityService);

    }
}
