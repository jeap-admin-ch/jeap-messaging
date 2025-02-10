package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.serde.SerdeUtils;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureService;
import com.amazonaws.services.schemaregistry.serializers.GlueSchemaRegistryKafkaSerializer;
import jakarta.annotation.Nullable;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.Map;

public class JeapGlueAvroSerializer implements Serializer<Object> {

    private final GlueSchemaRegistryKafkaSerializer delegate;
    private final JeapKafkaAvroSerdeCryptoConfig cryptoConfig;
    private final SignatureService signatureService;
    private boolean isKey;

    public JeapGlueAvroSerializer(AwsCredentialsProvider awsCredentialsProvider, JeapKafkaAvroSerdeCryptoConfig cryptoConfig, @Nullable SignatureService signatureService) {
        this.delegate = new GlueSchemaRegistryKafkaSerializer(awsCredentialsProvider, null);
        this.cryptoConfig = cryptoConfig;
        this.signatureService = signatureService;
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
        if (signatureService != null) {
            signatureService.injectSignature(headers, payload, isKey);
        }
        return SerdeUtils.encryptPayloadIfRequired(isKey, cryptoConfig, headers, record, payload);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
