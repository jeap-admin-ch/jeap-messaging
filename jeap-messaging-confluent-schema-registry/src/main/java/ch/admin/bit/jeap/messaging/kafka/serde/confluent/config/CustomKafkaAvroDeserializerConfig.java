package ch.admin.bit.jeap.messaging.kafka.serde.confluent.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

import java.util.Map;

/**
 * We cannot easily inherit from
 * {@link io.confluent.kafka.serializers.KafkaAvroDeserializerConfig} as it does
 * not provide a constructor taking a configDec value. Therefore we overwrite
 * {@link AbstractKafkaSchemaSerDeConfig} directly and apply the only setting from
 * {@link AbstractKafkaSchemaSerDeConfig} here as well.
 */
@SuppressWarnings("WeakerAccess")
public class CustomKafkaAvroDeserializerConfig extends AbstractKafkaSchemaSerDeConfig {
    public static final String DECRYPT_MESSAGES_CONFIG = "decrypt.messages";
    public static final boolean DECRYPT_MESSAGES_DEFAULT = false;
    public static final String DECRYPT_MESSAGES_DOC = "If true, messages are decrypted using the specified passphrase";

    public static final String DECRYPT_PASSPHRASE_CONFIG = "decrypt.passphrase";
    public static final String DECRYPT_PASSPHRASE_DEFAULT = "";
    public static final String DECRYPT_PASSPHRASE_DOC = "Passphrase used for decrypting Kafka messages";
    public static final String SCHEMA_REGISTRY_CLIENT = "jeap.schema.registry.client";
    public static final String JEAP_SERDE_CRYPTO_CONFIG = "jeap.serde.crypto.config";
    public static final String SPECIFIC_AVRO_KEY_TYPE = "specific.avro.key.type";
    public static final String SPECIFIC_AVRO_VALUE_TYPE = "specific.avro.value.type";

    public static final String JEAP_SIGNATURE_AUTHENTICITY_SERVICE = "jeap.serde.authentication.subscriber.service";

    public CustomKafkaAvroDeserializerConfig(Map<?, ?> props) {
        super(baseConfigDef(), props);
    }

    /**
     * This constructor can be used if further overwriting is needed
     */
    @SuppressWarnings("unused")
    protected CustomKafkaAvroDeserializerConfig(ConfigDef config, Map<?, ?> props) {
        super(config, props);
    }

    public static ConfigDef baseConfigDef() {
        return AbstractKafkaSchemaSerDeConfig.baseConfigDef()
                .define(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, Type.BOOLEAN,
                        KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_DEFAULT, Importance.LOW,
                        KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_DOC)
                .define(DECRYPT_MESSAGES_CONFIG, Type.BOOLEAN,
                        DECRYPT_MESSAGES_DEFAULT, Importance.LOW, DECRYPT_MESSAGES_DOC)
                .define(DECRYPT_PASSPHRASE_CONFIG, Type.STRING,
                        DECRYPT_PASSPHRASE_DEFAULT, Importance.LOW, DECRYPT_PASSPHRASE_DOC);
    }
}
