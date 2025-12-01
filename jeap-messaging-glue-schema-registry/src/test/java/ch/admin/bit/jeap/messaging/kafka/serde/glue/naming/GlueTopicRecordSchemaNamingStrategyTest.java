package ch.admin.bit.jeap.messaging.kafka.serde.glue.naming;

import ch.admin.bit.jeap.messaging.test.glue.avro.TestEmptyMessageKey;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlueTopicRecordSchemaNamingStrategyTest {

    private final GlueTopicRecordSchemaNamingStrategy strategy = new GlueTopicRecordSchemaNamingStrategy();

    @Test
    void getSchemaName() {
        String schemaName = strategy.getSchemaName("my-topic");

        assertThat(schemaName)
                .isEqualTo("my-topic");
    }

    @Test
    void getSchemaName_notAvro() {
        String schemaName = strategy.getSchemaName("my-topic", new Object());

        assertThat(schemaName)
                .isEqualTo("my-topic");
    }

    @Test
    void getSchemaName_avro() {
        String schemaName = strategy.getSchemaName("my-topic", createGenericRecord());

        assertThat(schemaName)
                .isEqualTo("my-topic-ch.admin.bit.jeap.messaging.test.glue.avro.TestEmptyMessageKey");
    }

    @Test
    void getSchemaName_keyAndNotKey() {
        String schemaName1 = strategy.getSchemaName("my-topic", createGenericRecord(), false);
        String schemaName2 = strategy.getSchemaName("my-topic", createGenericRecord(), true);

        assertThat(schemaName1)
                .isEqualTo("my-topic-ch.admin.bit.jeap.messaging.test.glue.avro.TestEmptyMessageKey");
        assertThat(schemaName2)
                .isEqualTo("my-topic-ch.admin.bit.jeap.messaging.test.glue.avro.TestEmptyMessageKey-key");
    }

    private GenericRecord createGenericRecord() {
        return TestEmptyMessageKey.newBuilder().setValue("test-key").build();
    }
}
