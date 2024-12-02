package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import com.amazonaws.services.schemaregistry.common.AWSDeserializerInput;
import com.amazonaws.services.schemaregistry.common.AWSSchemaRegistryClient;
import com.amazonaws.services.schemaregistry.common.configs.GlueSchemaRegistryConfiguration;
import com.amazonaws.services.schemaregistry.deserializers.GlueSchemaRegistryDeserializationFacade;
import com.amazonaws.services.schemaregistry.deserializers.GlueSchemaRegistryDeserializerDataParser;
import com.amazonaws.services.schemaregistry.exception.AWSSchemaRegistryException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.glue.model.GetSchemaVersionResponse;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class JeapGlueSchemaRegistryDeserializationFacade extends GlueSchemaRegistryDeserializationFacade {

    private final Class<?> specificClass;
    private final JeapCustomAvroDeserializer jeapCustomAvroDeserializer = new JeapCustomAvroDeserializer();

    private final LoadingCache<UUID, com.amazonaws.services.schemaregistry.common.Schema> schemaCache;

    private final AWSSchemaRegistryClient schemaRegistryClient;

    private static final GlueSchemaRegistryDeserializerDataParser DESERIALIZER_DATA_PARSER = GlueSchemaRegistryDeserializerDataParser.getInstance();

    public JeapGlueSchemaRegistryDeserializationFacade(@NonNull GlueSchemaRegistryConfiguration configuration, @NonNull AwsCredentialsProvider credentialsProvider, @NonNull Class<?> specificClass) {
        super(configuration, credentialsProvider);
        this.specificClass = specificClass;
        this.schemaRegistryClient = super.getSchemaRegistryClient();
        this.schemaCache = initializeSchemaCache();
    }

    @Override
    public Object deserialize(@NonNull AWSDeserializerInput deserializerInput) {
        final ByteBuffer buffer = deserializerInput.getBuffer();
        UUID schemaVersionId = DESERIALIZER_DATA_PARSER.getSchemaVersionId(buffer);
        com.amazonaws.services.schemaregistry.common.Schema writerSchema = retrieveWriterSchemaRegistrySchema(schemaVersionId);
        return jeapCustomAvroDeserializer.deserialize(buffer, writerSchema, retrieveReaderSchemaFromSpecificClass());
    }

    @SuppressWarnings("java:S112")
    private Schema retrieveReaderSchemaFromSpecificClass() {
        try {
            return ((SpecificRecord) this.specificClass.getDeclaredConstructor().newInstance()).getSchema();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Unable to retrieve reader schema from specific class {}", this.specificClass.getName(), e);
            throw new RuntimeException(e);
        }
    }

    private com.amazonaws.services.schemaregistry.common.Schema retrieveWriterSchemaRegistrySchema(UUID schemaVersionId) {
        try {
            return schemaCache.get(schemaVersionId);
        } catch (Exception e) {
            log.error("Unable to retrieve writer schema with uuid {}", schemaVersionId, e);
            throw new AWSSchemaRegistryException(e.getCause());
        }
    }

    private LoadingCache<UUID, com.amazonaws.services.schemaregistry.common.Schema> initializeSchemaCache() {
        return CacheBuilder
                .newBuilder()
                .maximumSize(super.getGlueSchemaRegistryConfiguration().getCacheSize())
                .refreshAfterWrite(super.getGlueSchemaRegistryConfiguration().getTimeToLiveMillis(), TimeUnit.MILLISECONDS)
                .build(new GlueSchemaRegistryDeserializationCacheLoader());
    }

    private class GlueSchemaRegistryDeserializationCacheLoader extends CacheLoader<UUID, com.amazonaws.services.schemaregistry.common.Schema> {
        @Override
        public com.amazonaws.services.schemaregistry.common.Schema load(UUID schemaVersionId) {
            GetSchemaVersionResponse response = schemaRegistryClient.getSchemaVersionResponse(schemaVersionId.toString());
            return new com.amazonaws.services.schemaregistry.common.Schema(response.schemaDefinition(), response.dataFormat().name(), getSchemaName(response.schemaArn()));
        }

        private String getSchemaName(String schemaArn) {
            Arn arn = Arn.fromString(schemaArn);
            String resource = arn.resourceAsString();
            String[] splitArray = resource.split("/");
            return splitArray[splitArray.length - 1];
        }
    }
}
