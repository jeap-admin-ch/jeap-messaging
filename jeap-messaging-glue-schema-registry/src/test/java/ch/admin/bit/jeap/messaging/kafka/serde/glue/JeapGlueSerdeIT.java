package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
        Deserializer<GenericData.Record> deserializer = kafkaAvroSerdeProvider.getGenericDataRecordDeserializer();

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
}