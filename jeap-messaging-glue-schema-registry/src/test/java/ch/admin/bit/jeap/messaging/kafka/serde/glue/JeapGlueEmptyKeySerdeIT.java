package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.test.glue.avro.TestEmptyMessageKey;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "jeap.messaging.kafka.expose-message-key-to-consumer=false")
class JeapGlueEmptyKeySerdeIT extends AbstractGlueSerdeTestBase {

    @Test
    void testEmptyKeySerdeRoundtrip() {
        UUID versionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(versionId, "test-topic-ch.admin.bit.jeap.messaging.test.glue.avro.TestEmptyMessageKey-key");
        stubGetSchemaVersionResponse(versionId, TestEmptyMessageKey.SCHEMA$.toString());

        Serializer<Object> serializer = kafkaAvroSerdeProvider.getKeySerializer();

        AvroMessageKey testKey = createTestEmptyMessageKey();
        byte[] serializedKey = serializer.serialize("test-topic", testKey);

        assertThat(serializedKey)
                .isNotNull();
    }

    private AvroMessageKey createTestEmptyMessageKey() {
        return TestEmptyMessageKey.newBuilder().setValue("test-key").build();
    }
}
