package ch.admin.bit.jeap.messaging.kafka.serde.confluent;

import ch.admin.bit.jeap.crypto.api.KeyId;
import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureService;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * This serializer can be used instead of {@link KafkaAvroSerializer}. Whenever
 * a new subject is created on the schema registry this extension will set the
 * compatibility mode to NONE. Compatibility in jeap-messaging can be changed
 * between versions to allow for expand/migrate/contract style evolution. Compatibility
 * is validated in the message type registry at development time.
 */
@SuppressWarnings("unused")
public class CustomKafkaAvroSerializer extends KafkaAvroSerializer {

    protected JeapKafkaAvroSerdeCryptoConfig cryptoConfig;

    private SignatureService signatureService;

    public CustomKafkaAvroSerializer() {
        super();
    }

    public CustomKafkaAvroSerializer(SchemaRegistryClient schemaRegistryClient, JeapKafkaAvroSerdeCryptoConfig cryptoConfig, SignatureService signatureService) {
        super(schemaRegistryClient);
        this.cryptoConfig = cryptoConfig;
        this.signatureService = signatureService;
    }

    public CustomKafkaAvroSerializer(SchemaRegistryClient schemaRegistryClient, JeapKafkaAvroSerdeCryptoConfig cryptoConfig, SignatureService signatureService, Map<String, ?> props) {
        super(schemaRegistryClient, props);
        this.cryptoConfig = cryptoConfig;
        this.signatureService = signatureService;
    }

    @Override
    public byte[] serialize(String topic, Object record) {
        if (record == null) {
            return null;
        } else {
            AvroSchema schema = new AvroSchema(
                    AvroSchemaUtils.getSchema(record, this.useSchemaReflection, this.avroReflectionAllowNull, this.removeJavaProperties, true));
            return this.serializeImpl(getSubjectName(topic, isKey, record, schema), record, schema);
        }
    }

    @Override
    public byte[] serialize(String topic, Headers headers, Object record) {
        if (record == null) {
            return null;
        } else {
            AvroSchema messageSchema = new AvroSchema(
                    AvroSchemaUtils.getSchema(record, this.useSchemaReflection, this.avroReflectionAllowNull, this.removeJavaProperties, true));
            byte[] payload = this.serializeImpl(this.getSubjectName(topic, isKey, record, messageSchema), record, messageSchema);
            if (signatureService != null) {
                signatureService.injectSignature(headers, payload, isKey);
            }
            if (!isKey && (cryptoConfig != null)) {
                Optional<KeyId> keyIdOptional = cryptoConfig.getKeyIdForMessageTypeName(messageSchema.rawSchema().getName());
                if (keyIdOptional.isPresent()) {
                    KeyId keyId = keyIdOptional.get();
                    byte[] encryptedPayload = cryptoConfig.getKeyIdCryptoService(keyId).encrypt(payload, keyId);
                    headers.add(
                            JeapKafkaAvroSerdeCryptoConfig.ENCRYPTED_VALUE_HEADER_NAME,
                            JeapKafkaAvroSerdeCryptoConfig.ENCRYPTED_VALUE_HEADER_TRUE);
                    return encryptedPayload;
                }
            }
            return payload;
        }
    }

    @Override
    protected byte[] serializeImpl(String subject, Object object, AvroSchema schema) throws SerializationException, InvalidConfigurationException {
        if (autoRegisterSchema) {
            registerNewObjectSchemaWithModeNone(subject, object);
        }
        return super.serializeImpl(subject, object, schema);
    }

    private void registerNewObjectSchemaWithModeNone(String subject, Object object) {
        // If there is no object we can ignore this call as will super.serializeImpl do.
        if (object == null) {
            return;
        }
        Schema schema = AvroSchemaUtils.getSchema(object);

        // Lets check if this schema is already in the registry. We can do this
        // additional call since the
        // registry is cached. So when we later go to super.serializeImpl this will not
        // do another rest call.
        // NOTE: The schema registry will actually return an Exception if the schema is
        // not present at all...
        try {
            int id = this.schemaRegistry.getId(subject, new AvroSchema(schema));
            if (id >= 0) {
                // This is already registered
                return;
            }
        } catch (RuntimeException | IOException e) {
            throw new SerializationException("Error serializing Avro message", e);
        } catch (RestClientException e2) {
            // This happen if there is no such schema yet, continue
        }

        // As we do not have this schema at the registry, we want to add it and set the
        // compatibility mode to NONE
        try {
            this.schemaRegistry.updateCompatibility(subject, "NONE");
            this.schemaRegistry.register(subject, new AvroSchema(schema));
        } catch (RuntimeException | IOException e) {
            throw new SerializationException("Error serializing Avro message", e);
        } catch (RestClientException e2) {
            throw new SerializationException("Could not register Avro schema", e2);
        }
    }
}
