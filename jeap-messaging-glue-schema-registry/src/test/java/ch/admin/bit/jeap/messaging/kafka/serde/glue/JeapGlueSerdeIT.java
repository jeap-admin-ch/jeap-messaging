package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.legacydecryption.LegacyMessageDecryptor;
import ch.admin.bit.jeap.messaging.kafka.legacydecryption.LegacyMessageEncryptor;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.config.properties.GlueKafkaAvroSerdeProperties;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // Avoid schema caching between tests
@SuppressWarnings({"resource", "SameParameterValue"})
class JeapGlueSerdeIT extends AbstractGlueSerdeTestBase {

    @Test
    void testMessageSerialization() {
        UUID versionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(versionId, "test-topic-TestEvent");
        stubGetSchemaVersionResponse(versionId, TEST_EVENT_AVRO_SCHEMA);

        Serializer<Object> serializer = kafkaAvroSerdeProvider.getValueSerializer();

        AvroMessage testEvent = createTestEvent();
        byte[] serializedRecord = serializer.serialize("test-topic", testEvent);

        assertThat(serializedRecord).isNotNull();
    }

    @Test
    void testMessageSerDeToGenericRecordRoundtrip() {
        UUID versionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(versionId, "test-topic2-TestEvent");
        stubGetSchemaVersionResponse(versionId, TEST_EVENT_AVRO_SCHEMA);

        Serializer<Object> serializer = kafkaAvroSerdeProvider.getValueSerializer();
        Deserializer<GenericData.Record> deserializer = kafkaAvroSerdeProvider.getGenericDataRecordDeserializerWithoutSignatureCheck();

        AvroMessage testEvent = createTestEvent();
        byte[] serializedRecord = serializer.serialize("test-topic2", testEvent);
        GenericData.Record deserializedMessage = deserializer.deserialize("test-topic2", serializedRecord);

        String domainEventVersion = (String) deserializedMessage.get("domainEventVersion");
        assertThat(domainEventVersion).isEqualTo("1.1.0");
    }

    @Test
    void testKeySerialization() {
        UUID versionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(versionId, "test-topic-TestMessageKey-key");
        stubGetSchemaVersionResponse(versionId, TEST_KEY_AVRO_SCHEMA);

        Serializer<Object> serializer = kafkaAvroSerdeProvider.getKeySerializer();

        AvroMessageKey testKey = createTestMessageKey();
        byte[] serializedKey = serializer.serialize("test-topic", testKey);

        assertThat(serializedKey).isNotNull();
    }

    @Test
    void testMessageSerDeRoundtrip() {
        UUID versionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(versionId, "test-topic2-TestEvent");
        stubGetSchemaVersionResponse(versionId, TEST_EVENT_AVRO_SCHEMA);

        Serializer<Object> serializer = kafkaAvroSerdeProvider.getValueSerializer();
        JeapGlueAvroDeserializer deserializer = new JeapGlueAvroDeserializer(awsCredentialsProvider, null);
        Map<String, Object> props = new HashMap<>(kafkaAvroSerdeProvider.getSerdeProperties().avroDeserializerProperties("default"));
        props.remove(GlueKafkaAvroSerdeProperties.JEAP_SIGNATURE_AUTHENTICITY_SERVICE);
        deserializer.configure(props, false);

        AvroMessage testEvent = createTestEvent();
        byte[] serializedRecord = serializer.serialize("test-topic2", testEvent);
        AvroMessage deserializedMessage = (AvroMessage) deserializer.deserialize("test-topic2", serializedRecord);

        assertThat(deserializedMessage)
                .isNotNull();
        assertThat(deserializedMessage.getIdentity().getId())
                .isEqualTo("id");
    }

    @Test
    void testMessageDeserialization_nifiDecryption() throws GeneralSecurityException {
        UUID versionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(versionId, "test-topic2-TestEvent");
        stubGetSchemaVersionResponse(versionId, TEST_EVENT_AVRO_SCHEMA);
        Serializer<Object> serializer = kafkaAvroSerdeProvider.getValueSerializer();
        String passphrase = "test-passphrase";
        LegacyMessageDecryptor legacyMessageDecryptor = new LegacyMessageDecryptor(passphrase);
        JeapGlueAvroDeserializer deserializer = createDeserializerWithNifiCompatibleDecyption(passphrase);
        AvroMessage testEvent = createTestEvent();

        byte[] serializedEncryptedRecord = LegacyMessageEncryptor.encryptMessage(serializer.serialize("test-topic2", testEvent), passphrase);
        AvroMessage deserializedMessage = (AvroMessage) deserializer.deserialize("test-topic2", serializedEncryptedRecord);

        assertThat(deserializedMessage)
                .isNotNull();
        assertThat(deserializedMessage.getIdentity().getId())
                .isEqualTo("id");
    }

    private JeapGlueAvroDeserializer createDeserializerWithNifiCompatibleDecyption(String passphrase) {
        Map<String, Object> props = new HashMap<>(kafkaAvroSerdeProvider.getSerdeProperties().avroDeserializerProperties("default"));
        props.remove(GlueKafkaAvroSerdeProperties.JEAP_SIGNATURE_AUTHENTICITY_SERVICE);
        props.put(JeapGlueAvroDeserializer.DECRYPT_MESSAGES_CONFIG, true);
        props.put(JeapGlueAvroDeserializer.DECRYPT_PASSPHRASE_CONFIG, passphrase);
        JeapGlueAvroDeserializer deserializer = new JeapGlueAvroDeserializer(awsCredentialsProvider, null);
        deserializer.configure(props, false);
        return deserializer;
    }
}
