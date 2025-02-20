package ch.admin.bit.jeap.messaging.kafka.serde.glue.config.properties;

import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.GlueProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.EmptyKeyDeserializer;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.JeapGlueAvroDeserializer;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.JeapGlueAvroSerializer;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.naming.GlueTopicRecordSchemaNamingStrategy;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureAuthenticityService;
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import com.amazonaws.services.schemaregistry.utils.AvroRecordType;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.glue.model.Compatibility;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * See <a href="https://docs.aws.amazon.com/glue/latest/dg/schema-registry-gs.html">AWS Docs for Glue</a>
 */
public class GlueKafkaAvroSerdeProperties implements KafkaAvroSerdeProperties {

    public static final String GLUE_AWS_CREDENTIALS_PROVIDER = "glueAwsCredentialsProvider";

    public static final String JEAP_CRYPTO_CONFIG = "jeapKafkaAvroSerdeCryptoConfig";
    public static final String JEAP_SIGNATURE_AUTHENTICITY_SERVICE = "jeapSignatureAuthenticityService";

    private final KafkaProperties kafkaProperties;
    private final GlueProperties configProperties;
    private final AwsCredentialsProvider glueAwsCredentialsProvider;
    private final JeapKafkaAvroSerdeCryptoConfig cryptoConfig;
    private final SignatureAuthenticityService signatureAuthenticityService;

    public GlueKafkaAvroSerdeProperties(KafkaProperties kafkaProperties, GlueProperties configProperties, AwsCredentialsProvider glueAwsCredentialsProvider,
                                        JeapKafkaAvroSerdeCryptoConfig cryptoConfig, SignatureAuthenticityService signatureAuthenticityService) {
        this.kafkaProperties = kafkaProperties;
        this.configProperties = configProperties;
        this.glueAwsCredentialsProvider = glueAwsCredentialsProvider;
        this.cryptoConfig = cryptoConfig;
        this.signatureAuthenticityService = signatureAuthenticityService;
    }

    @Override
    public Map<String, Object> avroSerializerProperties(String clusterName) {
        validateSerializerProperties();

        Map<String, Object> props = new HashMap<>();

        addCommonProps(props);

        String registryName = configProperties.getRegistryName();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, JeapGlueAvroSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JeapGlueAvroSerializer.class.getName());
        props.put(AWSSchemaRegistryConstants.DATA_FORMAT, "AVRO");
        Map<String, String> metadata = createMetadata();
        props.put(AWSSchemaRegistryConstants.METADATA, metadata);
        props.put(AWSSchemaRegistryConstants.DESCRIPTION, getRegistryDescription(registryName));
        props.put(AWSSchemaRegistryConstants.COMPRESSION_TYPE, AWSSchemaRegistryConstants.COMPRESSION.NONE.name());
        props.put(AWSSchemaRegistryConstants.SCHEMA_NAMING_GENERATION_CLASS, GlueTopicRecordSchemaNamingStrategy.class.getName());
        props.put(AWSSchemaRegistryConstants.COMPATIBILITY_SETTING, Compatibility.NONE);
        props.put(AWSSchemaRegistryConstants.SCHEMA_AUTO_REGISTRATION_SETTING, kafkaProperties.isAutoRegisterSchema());
        props.put(AWSSchemaRegistryConstants.REGISTRY_NAME, registryName);

        return props;
    }

    @Override
    public Map<String, Object> avroDeserializerProperties(String clusterName) {
        Map<String, Object> props = new HashMap<>();

        addCommonProps(props);
        props.put(AWSSchemaRegistryConstants.AVRO_RECORD_TYPE, AvroRecordType.SPECIFIC_RECORD.getName()); // Deserialize Avro to specific Java type
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JeapGlueAvroDeserializer.class);
        props.put(GLUE_AWS_CREDENTIALS_PROVIDER, this.glueAwsCredentialsProvider);

        if (this.cryptoConfig != null) {
            props.put(JEAP_CRYPTO_CONFIG, this.cryptoConfig);
        }
        if (this.signatureAuthenticityService != null) {
            props.put(JEAP_SIGNATURE_AUTHENTICITY_SERVICE, this.signatureAuthenticityService);
        }

        if (kafkaProperties.isExposeMessageKeyToConsumer()) {
            props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, JeapGlueAvroDeserializer.class);
        } else {
            props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, EmptyKeyDeserializer.class);
        }

        return props;
    }

    private void addCommonProps(Map<String, Object> props) {
        String awsRegion = configProperties.getRegion();
        URI glueEndpoint = configProperties.getEndpoint();
        props.put(AWSSchemaRegistryConstants.CACHE_SIZE, "200"); // default value is 200
        props.put(AWSSchemaRegistryConstants.CACHE_TIME_TO_LIVE_MILLIS, "86400000"); // Default is 86400000 (24 Hours)
        props.put(AWSSchemaRegistryConstants.AWS_REGION, awsRegion);
        if (glueEndpoint != null) {
            props.put(AWSSchemaRegistryConstants.AWS_ENDPOINT, glueEndpoint.toString());
        }
    }

    private Map<String, String> createMetadata() {
        HashMap<String, String> metadata = new HashMap<>(); // Note: This MUST be a HashMap
        metadata.put("registered-by-service", kafkaProperties.getServiceName());
        metadata.put("registered-by-system", kafkaProperties.getSystemName());
        return metadata;
    }

    private String getRegistryDescription(String registryName) {
        return registryName + " " + kafkaProperties.getServiceName();
    }


    private void validateSerializerProperties() {
        validatePropertyHasValue("systemName", kafkaProperties.getSystemName());
        validatePropertyHasValue("serviceName", kafkaProperties.getServiceName());
    }

    private void validatePropertyHasValue(String propertyName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Property jeap.messaging.kafka.%s is missing a valid value. This property needs to be set for applications using a glue schema registry."
                            .formatted(propertyName));
        }
    }
}
