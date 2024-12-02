package ch.admin.bit.jeap.messaging.kafka.serde.confluent.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

import java.util.Map;

/**
 * We cannot easily inherit from
 * {@link io.confluent.kafka.serializers.KafkaAvroSerializerConfig} as it does
 * not provide a constructor taking a configDec value. Therefore we overwrite
 * {@link AbstractKafkaSchemaSerDeConfig} directly.
 */
@SuppressWarnings("WeakerAccess")
public class CustomKafkaAvroSerializerConfig extends AbstractKafkaSchemaSerDeConfig {
    public static final String ENCRYPT_MESSAGES_CONFIG = "encrypt.messages";
    public static final boolean ENCRYPT_MESSAGES_DEFAULT = false;
    public static final String ENCRYPT_MESSAGES_DOC = "If true, messages are encrypted using the specified passphrase";

    public static final String ENCRYPT_PASSPHRASE_CONFIG = "encrypt.passphrase";
    public static final String ENCRYPT_PASSPHRASE_DEFAULT = "";
    public static final String ENCRYPT_PASSPHRASE_DOC = "Passphrase used for encrypting Kafka messages";


    public CustomKafkaAvroSerializerConfig(Map<?, ?> props) {
        super(baseConfigDef(), props);
    }

    /**
     * This constructor can be used if further overwriting is needed
     */
    @SuppressWarnings("unused")
    public CustomKafkaAvroSerializerConfig(ConfigDef config, Map<?, ?> props) {
        super(config, props);
    }

    public static ConfigDef baseConfigDef() {
        return AbstractKafkaSchemaSerDeConfig.baseConfigDef()
                .define(ENCRYPT_MESSAGES_CONFIG, Type.BOOLEAN,
                        ENCRYPT_MESSAGES_DEFAULT, Importance.LOW, ENCRYPT_MESSAGES_DOC)
                .define(ENCRYPT_PASSPHRASE_CONFIG, Type.STRING,
                        ENCRYPT_PASSPHRASE_DEFAULT, Importance.LOW, ENCRYPT_PASSPHRASE_DOC);
    }
}
