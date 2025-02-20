package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import ch.admin.bit.jeap.kafka.SerializedMessageReceiver;
import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.serde.SerdeUtils;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.config.properties.GlueKafkaAvroSerdeProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureAuthenticityService;
import com.amazonaws.services.schemaregistry.common.configs.GlueSchemaRegistryConfiguration;
import com.amazonaws.services.schemaregistry.common.configs.UserAgents;
import com.amazonaws.services.schemaregistry.deserializers.GlueSchemaRegistryDeserializationFacade;
import com.amazonaws.services.schemaregistry.deserializers.GlueSchemaRegistryKafkaDeserializer;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.Map;

@Slf4j
public class JeapGlueAvroDeserializer implements Deserializer<Object> {

    public static final String SPECIFIC_AVRO_KEY_TYPE = "specific.avro.key.type";
    public static final String SPECIFIC_AVRO_VALUE_TYPE = "specific.avro.value.type";

    private GlueSchemaRegistryKafkaDeserializer delegate;
    private SignatureAuthenticityService signatureAuthenticityService;
    private JeapKafkaAvroSerdeCryptoConfig cryptoConfig;
    private boolean isKey;

    public JeapGlueAvroDeserializer() {
        this(null, null);
    }

    public JeapGlueAvroDeserializer(AwsCredentialsProvider awsCredentialsProvider, @Nullable SignatureAuthenticityService signatureAuthenticityService) {
        this.delegate = new GlueSchemaRegistryKafkaDeserializer(awsCredentialsProvider, null);
        this.signatureAuthenticityService = signatureAuthenticityService;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        addCredentialsProviderIfMissing(configs);
        addCryptoConfig(configs);
        setSignatureAuthenticityService(configs);
        defineSpecificDeserializerIfSpecificClassIsConfigured(configs, isKey);
        this.delegate.configure(configs, isKey);
        this.isKey = isKey;
    }


    private void addCredentialsProviderIfMissing(Map<String, ?> configs) {
        if (this.delegate.getCredentialProvider() == null) {
            log.debug("Add missing credentialsProvider to GlueSchemaRegistryKafkaDeserializer");
            AwsCredentialsProvider awsCredentialsProvider = (AwsCredentialsProvider) configs.get(GlueKafkaAvroSerdeProperties.GLUE_AWS_CREDENTIALS_PROVIDER);
            this.delegate = new GlueSchemaRegistryKafkaDeserializer(awsCredentialsProvider, null);
        }
    }

    private void addCryptoConfig(Map<String, ?> configs) {
        if (configs.get(GlueKafkaAvroSerdeProperties.JEAP_CRYPTO_CONFIG) != null) {
            this.cryptoConfig = (JeapKafkaAvroSerdeCryptoConfig) configs.get(GlueKafkaAvroSerdeProperties.JEAP_CRYPTO_CONFIG);
        }
    }

    private void setSignatureAuthenticityService(Map<String, ?> configs) {
        if (configs.get(GlueKafkaAvroSerdeProperties.JEAP_SIGNATURE_AUTHENTICITY_SERVICE) != null) {
            this.signatureAuthenticityService = (SignatureAuthenticityService) configs.get(GlueKafkaAvroSerdeProperties.JEAP_SIGNATURE_AUTHENTICITY_SERVICE);
        }
    }

    @SuppressWarnings("java:S112")
    private void defineSpecificDeserializerIfSpecificClassIsConfigured(Map<String, ?> configs, boolean isKey) {
        if (configs.containsKey(getSpecificAvroKeyFromType(isKey))) {
            String specificAvroType = (String) configs.get(getSpecificAvroKeyFromType(isKey));
            log.info("Configuring specific avro specificAvroType {}", specificAvroType);
            Class<?> specificAvroTypeClass;
            try {
                specificAvroTypeClass = Class.forName(specificAvroType);
            } catch (ClassNotFoundException e) {
                log.error("Unable to instantiate class from type {}", specificAvroType, e);
                throw new RuntimeException(e);
            }

            GlueSchemaRegistryConfiguration glueSchemaRegistryConfiguration = new GlueSchemaRegistryConfiguration(configs);
            glueSchemaRegistryConfiguration.setUserAgentApp(UserAgents.KAFKA);
            GlueSchemaRegistryDeserializationFacade facade = new JeapGlueSchemaRegistryDeserializationFacade(glueSchemaRegistryConfiguration, this.delegate.getCredentialProvider(), specificAvroTypeClass);
            this.delegate.setGlueSchemaRegistryDeserializationFacade(facade);
        }
    }

    private String getSpecificAvroKeyFromType(boolean isKey) {
        if (isKey) {
            return SPECIFIC_AVRO_KEY_TYPE;
        } else {
            return SPECIFIC_AVRO_VALUE_TYPE;
        }
    }

    @Override
    public Object deserialize(String topic, byte[] data) {
        return deserialize(topic, null, data);
    }

    @Override
    public Object deserialize(String topic, Headers headers, byte[] originalBytes) {
        byte[] possiblyDecryptedBytes = SerdeUtils.decryptIfEncrypted(isKey, cryptoConfig, topic, originalBytes, headers);
        Object result = delegate.deserialize(topic, headers, possiblyDecryptedBytes);
        if (result instanceof SerializedMessageReceiver smr) {
            // Note: The original message bytes must be in the original wire format, i.e. encrypted if applicable
            // The original message bytes are sent to the error handler on errors and must stay encrypted in this case
            smr.setSerializedMessage(originalBytes);
        }
        if (signatureAuthenticityService != null) {
            if (isKey) {
                signatureAuthenticityService.checkAuthenticityKey(headers, possiblyDecryptedBytes);
            } else {
                signatureAuthenticityService.checkAuthenticityValue(result, headers, possiblyDecryptedBytes);
            }
        }

        return result;
    }

    @Override
    public void close() {
        delegate.close();
    }
}
