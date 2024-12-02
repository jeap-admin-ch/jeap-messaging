package ch.admin.bit.jeap.messaging.kafka.serde.confluent.config;

import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.EmptyKeyDeserializer;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.CustomKafkaAvroDeserializer;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.CustomKafkaAvroSerializer;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static ch.admin.bit.jeap.messaging.kafka.properties.PropertyRequirements.requireNonNullValue;

public class KafkaConfluentAvroSerdeProperties implements KafkaAvroSerdeProperties {

    private final KafkaProperties kafkaProperties;
    private final JeapKafkaAvroSerdeCryptoConfig cryptoConfig;
    private SchemaRegistryClient schemaRegistryClient;

    public KafkaConfluentAvroSerdeProperties(KafkaProperties kafkaProperties, JeapKafkaAvroSerdeCryptoConfig cryptoConfig) {
        this.kafkaProperties = kafkaProperties;
        this.cryptoConfig = cryptoConfig;
    }

    @Override
    public Map<String, Object> avroSerializerProperties(String clusterName) {
        Map<String, Object> props = new HashMap<>();
        addCommonProperties(clusterName, props);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, CustomKafkaAvroSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CustomKafkaAvroSerializer.class);
        if (kafkaProperties.isUseSchemaRegistry()) {
            props.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, kafkaProperties.isAutoRegisterSchema());
        }
        props.put(AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class.getName());
        props.put(AbstractKafkaSchemaSerDeConfig.KEY_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class.getName());
        return props;
    }

    @Override
    public Map<String, Object> avroDeserializerProperties(String clusterName) {
        Map<String, Object> props = new HashMap<>();
        addCommonProperties(clusterName, props);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, CustomKafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        if (kafkaProperties.isExposeMessageKeyToConsumer()) {
            props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, CustomKafkaAvroDeserializer.class);
        } else {
            props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, EmptyKeyDeserializer.class);
        }
        addSchemaRegistryClient(props);
        if (this.cryptoConfig != null) {
            props.put(CustomKafkaAvroDeserializerConfig.JEAP_SERDE_CRYPTO_CONFIG, this.cryptoConfig);
        }
        return props;
    }

    private void addCommonProperties(String clusterName, Map<String, Object> props) {
        if (kafkaProperties.isUseSchemaRegistry()) {
            props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                    requireNonNullValue(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                            kafkaProperties.getSchemaRegistryUrl(clusterName)));
            if (StringUtils.hasText(kafkaProperties.getSchemaRegistryUsername(clusterName))) {
                props.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
                props.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, kafkaProperties.getSchemaRegistryUsername(clusterName) + ":" + kafkaProperties.getSchemaRegistryPassword(clusterName));
            }
        } else {
            props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://none");
            props.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);
        }
    }

    private void addSchemaRegistryClient(Map<String, Object> props) {
        if (this.schemaRegistryClient == null) {
            CustomKafkaAvroSerializerConfig serializerConfig = new CustomKafkaAvroSerializerConfig(props);
            this.schemaRegistryClient = SchemaRegistryClientUtil.createSchemaRegistryClient(serializerConfig);
        }
        props.put(CustomKafkaAvroDeserializerConfig.SCHEMA_REGISTRY_CLIENT, this.schemaRegistryClient);
    }

}
