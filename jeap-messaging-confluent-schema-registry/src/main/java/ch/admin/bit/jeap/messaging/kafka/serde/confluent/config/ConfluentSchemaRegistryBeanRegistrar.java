package ch.admin.bit.jeap.messaging.kafka.serde.confluent.config;

import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.ClusterProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.CustomKafkaAvroDeserializer;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.CustomKafkaAvroSerializer;
import ch.admin.bit.jeap.messaging.kafka.spring.AbstractSchemaRegistryBeanRegistrar;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

/**
 * Registers a {@link KafkaAvroSerdeProvider} for each configured kafka cluster if the confluent schema registry is
 * used for the cluster. This is the case if either useSchemaRegistry=false (use confluent mock schema registry)
 * or a schemaRegistryUrl is configured for the cluster.
 */
class ConfluentSchemaRegistryBeanRegistrar extends AbstractSchemaRegistryBeanRegistrar {
    @Override
    protected boolean shouldRegisterSchemaRegistryBeansForCluster(ClusterProperties clusterProperties) {
        boolean useMockConfluentSchemaRegistry = !kafkaProperties.isUseSchemaRegistry();
        return useMockConfluentSchemaRegistry || hasText(clusterProperties.getSchemaRegistryUrl());
    }

    @Override
    protected KafkaAvroSerdeProvider createKafkaAvroSerializerProvider(String clusterName, JeapKafkaAvroSerdeCryptoConfig cryptoConfig) {
        KafkaConfluentAvroSerdeProperties serdeProperties = createKafkaConfluentAvroSerdeProperties(kafkaProperties, cryptoConfig);
        CustomKafkaAvroSerializerConfig serializerConfig = new CustomKafkaAvroSerializerConfig(serdeProperties.avroSerializerProperties(clusterName));
        SchemaRegistryClient registryClient = SchemaRegistryClientUtil.createSchemaRegistryClient(serializerConfig);

        KafkaAvroSerializer valueSerializer = new CustomKafkaAvroSerializer(registryClient, cryptoConfig);
        valueSerializer.configure(serdeProperties.avroSerializerProperties(clusterName), false);
        KafkaAvroSerializer keySerializer = new CustomKafkaAvroSerializer(registryClient, null);
        keySerializer.configure(serdeProperties.avroSerializerProperties(clusterName), true);

        Deserializer<GenericData.Record> genericDataRecordDeserializer =
                createGenericRecordDataDeserializer(clusterName, registryClient, serdeProperties);

        return new KafkaAvroSerdeProvider(valueSerializer, keySerializer, genericDataRecordDeserializer, serdeProperties);
    }

    private KafkaConfluentAvroSerdeProperties createKafkaConfluentAvroSerdeProperties(KafkaProperties kafkaProperties, JeapKafkaAvroSerdeCryptoConfig cryptoConfig) {
        return new KafkaConfluentAvroSerdeProperties(kafkaProperties, cryptoConfig);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Deserializer<GenericData.Record> createGenericRecordDataDeserializer(String clusterName,
                                                                                        SchemaRegistryClient registryClient,
                                                                                        KafkaConfluentAvroSerdeProperties serdeProperties) {
        Deserializer genericRecordValueDeserializer = new CustomKafkaAvroDeserializer(registryClient, null);
        Map<String, Object> props = new HashMap<>(serdeProperties.avroDeserializerProperties(clusterName));
        // Deserializing to GenericData.Record instead of SpecificRecordBase
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);
        genericRecordValueDeserializer.configure(props, false);
        return genericRecordValueDeserializer;
    }
}