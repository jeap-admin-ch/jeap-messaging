package ch.admin.bit.jeap.messaging.kafka.serde.glue.config;

import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.AwsProperties;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.ClusterProperties;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.GlueProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.JeapGlueAvroDeserializer;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.JeapGlueAvroSerializer;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.config.auth.GlueAssumeRoleAuthProvider;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.config.auth.GlueAuthProvider;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.config.properties.GlueKafkaAvroSerdeProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.AbstractSchemaRegistryBeanRegistrar;
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import com.amazonaws.services.schemaregistry.utils.AvroRecordType;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GlueSchemaRegistryBeanRegistrar extends AbstractSchemaRegistryBeanRegistrar {
    private static final boolean IS_VALUE = false;
    private static final boolean IS_KEY = true;

    @Override
    protected boolean shouldRegisterSchemaRegistryBeansForCluster(ClusterProperties clusterProperties) {
        AwsProperties aws = clusterProperties.getAws();
        return aws != null &&
                aws.getGlue() != null &&
                aws.getGlue().isActive();
    }

    @Override
    protected KafkaAvroSerdeProvider createKafkaAvroSerializerProvider(String clusterName, JeapKafkaAvroSerdeCryptoConfig cryptoConfig) {
        GlueProperties glueProperties = kafkaProperties.clusterProperties(clusterName).orElseThrow()
                .getAws().getGlue();
        AwsCredentialsProvider awsCredentialsProvider = beanFactory.getBean(AwsCredentialsProvider.class);

        GlueAuthProvider glueAuthProvider = glueAuthProvider(awsCredentialsProvider, glueProperties);
        AwsCredentialsProvider glueCredentialsProvider = glueAuthProvider.getAwsCredentialsProvider();

        GlueKafkaAvroSerdeProperties serdeProperties = kafkaAvroSerdeProperties(kafkaProperties, glueProperties, glueCredentialsProvider, cryptoConfig);
        log.debug("Creating Avro serializers with config {}", serdeProperties.avroSerializerProperties(clusterName));
        log.debug("Creating Avro deserializers with config {}", serdeProperties.avroDeserializerProperties(clusterName));


        Serializer<Object> valueSerializer = new JeapGlueAvroSerializer(glueCredentialsProvider, cryptoConfig);
        valueSerializer.configure(serdeProperties.avroSerializerProperties(clusterName), IS_VALUE);

        Serializer<Object> keySerializer = new JeapGlueAvroSerializer(glueCredentialsProvider, cryptoConfig);
        keySerializer.configure(serdeProperties.avroSerializerProperties(clusterName), IS_KEY);

        Deserializer<GenericData.Record> genericRecordDataDeserializer = createGenericRecordDataDeserializer(clusterName, glueCredentialsProvider, serdeProperties);

        return new KafkaAvroSerdeProvider(valueSerializer, keySerializer, genericRecordDataDeserializer, serdeProperties);
    }

    private GlueKafkaAvroSerdeProperties kafkaAvroSerdeProperties(KafkaProperties kafkaProperties, GlueProperties glueProperties, AwsCredentialsProvider glueAwsCredentialsProvider, JeapKafkaAvroSerdeCryptoConfig cryptoConfig) {
        return new GlueKafkaAvroSerdeProperties(kafkaProperties, glueProperties, glueAwsCredentialsProvider, cryptoConfig);
    }

    private GlueAuthProvider glueAuthProvider(AwsCredentialsProvider awsCredentialsProvider, GlueProperties glueProperties) {
        if (glueProperties.useAssumeRoleForAuth()) {
            String sessionName = environment.getProperty("spring.application.name");
            return new GlueAssumeRoleAuthProvider(awsCredentialsProvider, glueProperties, sessionName);
        } else {
            return () -> awsCredentialsProvider;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Deserializer<GenericData.Record> createGenericRecordDataDeserializer(String clusterName,
                                                                                        AwsCredentialsProvider glueCredentialsProvider,
                                                                                        GlueKafkaAvroSerdeProperties serdeProperties) {
        Deserializer genericDataRecordDeserializer = new JeapGlueAvroDeserializer(glueCredentialsProvider);
        Map<String, Object> props = new HashMap<>(serdeProperties.avroDeserializerProperties(clusterName));
        props.put(AWSSchemaRegistryConstants.AVRO_RECORD_TYPE, AvroRecordType.GENERIC_RECORD.getName());
        genericDataRecordDeserializer.configure(props, IS_VALUE);
        return genericDataRecordDeserializer;
    }
}
