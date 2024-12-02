package ch.admin.bit.jeap.messaging.kafka.serde.confluent;

import ch.admin.bit.jeap.kafka.SerializedMessageReceiver;
import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.serde.SerdeUtils;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.config.CustomKafkaAvroDeserializerConfig;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;

import java.util.Map;

/**
 * This deserializer can be used instead of {@link KafkaAvroDeserializer}. You can define a static schema with whom all
 * Events will be deserialized. This can e.g. be used if you are only interested on a subset of attributes but on
 * multiple event types. Then you can define here a schema containing only those shared attributes. It also supports
 * reading encrypted Kafka messages.
 * You can configure this class by the additional attributes in {@link CustomKafkaAvroDeserializerConfig}
 */
@Slf4j
public class CustomKafkaAvroDeserializer extends KafkaAvroDeserializer {
    // configurable properties
    protected MessageEncryptor nifiCompatibleMessageEncryptor;

    protected JeapKafkaAvroSerdeCryptoConfig cryptoConfig;

    public CustomKafkaAvroDeserializer() {
        super();
    }

    public CustomKafkaAvroDeserializer(SchemaRegistryClient schemaRegistryClient, JeapKafkaAvroSerdeCryptoConfig cryptoConfig) {
        super(schemaRegistryClient);
        this.cryptoConfig = cryptoConfig;
    }

    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
        configureFromPropertyMap(props);

        Class<?> valueType = null;
        if (isKey) {
            if (props.containsKey(CustomKafkaAvroDeserializerConfig.SPECIFIC_AVRO_KEY_TYPE)) {
                try {
                    valueType = Class.forName((String) props.get(CustomKafkaAvroDeserializerConfig.SPECIFIC_AVRO_KEY_TYPE));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

        } else {
            if (props.containsKey(CustomKafkaAvroDeserializerConfig.SPECIFIC_AVRO_VALUE_TYPE)) {
                try {
                    valueType = Class.forName((String) props.get(CustomKafkaAvroDeserializerConfig.SPECIFIC_AVRO_VALUE_TYPE));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        configure(deserializerConfig(props), valueType);
    }

    private void configureFromPropertyMap(Map<String, ?> props) {
        this.schemaRegistry = (SchemaRegistryClient) props.get(CustomKafkaAvroDeserializerConfig.SCHEMA_REGISTRY_CLIENT);
        this.ticker = ticker(this.schemaRegistry);
        this.cryptoConfig = (JeapKafkaAvroSerdeCryptoConfig) props.get(CustomKafkaAvroDeserializerConfig.JEAP_SERDE_CRYPTO_CONFIG);

        CustomKafkaAvroDeserializerConfig customConfig = new CustomKafkaAvroDeserializerConfig(props);
        if (customConfig.getBoolean(CustomKafkaAvroDeserializerConfig.DECRYPT_MESSAGES_CONFIG)) {
            String encryptPassphrase = customConfig.getString(CustomKafkaAvroDeserializerConfig.DECRYPT_PASSPHRASE_CONFIG);
            nifiCompatibleMessageEncryptor = new MessageEncryptor(encryptPassphrase);
        }
    }

    @Override
    public Object deserialize(String topic, byte[] bytes) {
        return deserialize(topic, null, bytes);
    }

    @Override
    public Object deserialize(String topic, Headers headers, byte[] originalBytes) {
        boolean messageEncryptedWithJeapCrypto = SerdeUtils.isMessageEncryptedWithJeapCrypto(isKey, headers);
        boolean nifiCompatibleDecryptionEnabledForDeserializer = isNifiCompatibleDecryptionEnabledForDeserializer();
        validateOnlyOneDecryptionMechanismActive(messageEncryptedWithJeapCrypto, nifiCompatibleDecryptionEnabledForDeserializer, topic);

        byte[] possiblyDecryptedBytes;
        if (messageEncryptedWithJeapCrypto) {
            possiblyDecryptedBytes = SerdeUtils.decryptWithJeapCrypto(cryptoConfig, topic, originalBytes);
        } else if (nifiCompatibleDecryptionEnabledForDeserializer) {
            possiblyDecryptedBytes = nifiCompatibleMessageEncryptor.decryptMessage(originalBytes);
        } else {
            possiblyDecryptedBytes = originalBytes;
        }

        Object result = super.deserialize(possiblyDecryptedBytes);
        if (result instanceof SerializedMessageReceiver smr) {
            // Note: The original message bytes must be in the original wire format, i.e. encrypted if applicable
            // The original message bytes are sent to the error handler on errors and must stay encrypted in this case
            smr.setSerializedMessage(originalBytes);
        }

        return result;
    }

    private void validateOnlyOneDecryptionMechanismActive(boolean messageEncryptedWithJeapCrypto, boolean nifiCompatibleDecryptionEnabledForDeserializer, String topic) {
        if (messageEncryptedWithJeapCrypto && nifiCompatibleDecryptionEnabledForDeserializer) {
            throw new IllegalStateException("The headers of a message on topic '" + topic + "' indicate that the received " +
                                            " message is encrypted, and Nifi-compatible decryption is enabled as well for" +
                                            " this deserializer - only one of both can be enabled at the same time.");
        }
    }

    private boolean isNifiCompatibleDecryptionEnabledForDeserializer() {
        return nifiCompatibleMessageEncryptor != null;
    }

}
