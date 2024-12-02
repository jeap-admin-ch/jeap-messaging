package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.serde.SerdeUtils;
import com.amazonaws.services.schemaregistry.serializers.GlueSchemaRegistryKafkaSerializer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.Map;

public class JeapGlueAvroSerializer implements Serializer<Object> {

    private final GlueSchemaRegistryKafkaSerializer delegate;
    private final JeapKafkaAvroSerdeCryptoConfig cryptoConfig;
    private boolean isKey;

    public JeapGlueAvroSerializer() {
        this(null, null);
    }

    public JeapGlueAvroSerializer(AwsCredentialsProvider awsCredentialsProvider, JeapKafkaAvroSerdeCryptoConfig cryptoConfig) {
        this.delegate = new GlueSchemaRegistryKafkaSerializer(awsCredentialsProvider, null);
        this.cryptoConfig = cryptoConfig;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        this.delegate.configure(configs, isKey);
        this.isKey = isKey;
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        return serialize(topic, null, data);
    }

    @Override
    public byte[] serialize(String topic, Headers headers, Object record) {
        if (record == null) {
            return null;
        }

        byte[] payload = delegate.serialize(topic, headers, record);
        return SerdeUtils.encryptPayloadIfRequired(isKey, cryptoConfig, headers, record, payload);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
