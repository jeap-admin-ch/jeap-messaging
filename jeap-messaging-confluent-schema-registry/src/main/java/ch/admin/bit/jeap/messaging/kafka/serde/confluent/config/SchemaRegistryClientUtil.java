package ch.admin.bit.jeap.messaging.kafka.serde.confluent.config;

import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;

import java.util.List;

public class SchemaRegistryClientUtil {
    public static SchemaRegistryClient createSchemaRegistryClient(KafkaAvroSerializerConfig serializerConfig) {
        SchemaProvider provider = new AvroSchemaProvider();
        String mockScope = MockSchemaRegistry.validateAndMaybeGetMockScope(serializerConfig.getSchemaRegistryUrls());
        if (mockScope != null) {
            return MockSchemaRegistry.getClientForScope(mockScope, List.of(provider));
        } else {
            return createSchemaRegistry(serializerConfig, provider);
        }
    }

    private static SchemaRegistryClient createSchemaRegistry(KafkaAvroSerializerConfig serializerConfig, SchemaProvider provider) {
        return new CachedSchemaRegistryClient(
                serializerConfig.getSchemaRegistryUrls(),
                serializerConfig.getMaxSchemasPerSubject(),
                List.of(provider),
                serializerConfig.originalsWithPrefix(""),
                serializerConfig.requestHeaders());
    }
}
