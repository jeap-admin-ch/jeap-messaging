package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import com.amazonaws.services.schemaregistry.deserializers.GlueSchemaRegistryDeserializerDataParser;
import com.amazonaws.services.schemaregistry.utils.AVROUtils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

@Slf4j
@Getter
public class JeapCustomAvroDeserializer {

    private static final AVROUtils AVRO_UTILS = AVROUtils.getInstance();
    private static final GlueSchemaRegistryDeserializerDataParser DESERIALIZER_DATA_PARSER = GlueSchemaRegistryDeserializerDataParser.getInstance();

    private static final long MAX_DATUM_READER_CACHE_SIZE = 100;

    @NonNull
    protected final LoadingCache<DatumReaderCacheKey, DatumReader<Object>> datumReaderCache;

    @Builder
    public JeapCustomAvroDeserializer() {
        this.datumReaderCache =
            CacheBuilder
                .newBuilder()
                .maximumSize(MAX_DATUM_READER_CACHE_SIZE)
                .build(new DatumReaderCache());
    }

    @SuppressWarnings("java:S112")
    public Object deserialize(@NonNull ByteBuffer buffer, com.amazonaws.services.schemaregistry.common.Schema schema, Schema readerSchema) {

        try {
            byte[] data = DESERIALIZER_DATA_PARSER.getPlainData(buffer);
            log.debug("Length of actual message: {}", data.length);

            Schema writerSchema = AVRO_UTILS.parseSchema(schema.getSchemaDefinition());
            DatumReader<Object> datumReader = datumReaderCache.get(new DatumReaderCacheKey(writerSchema, readerSchema));

            BinaryDecoder binaryDecoder = getBinaryDecoder(data, data.length);
            Object result = datumReader.read(null, binaryDecoder);

            log.debug("Finished de-serializing Avro message");
            return result;
        } catch (IOException | ExecutionException e) {
            log.error("Unable to deserialize message with writer schema {} and reader schema {}", schema.getSchemaName(), readerSchema.getName(), e);
            throw new RuntimeException(e);
        }

    }

    private BinaryDecoder getBinaryDecoder(byte[] data, int end) {
        return DecoderFactory.get().binaryDecoder(data, 0, end, null);
    }

    private static class DatumReaderCache extends CacheLoader<DatumReaderCacheKey, DatumReader<Object>> {
        @Override
        public DatumReader<Object> load(DatumReaderCacheKey key) throws Exception {
            return JeapCustomDatumReaderFactory.from(key.writer(), key.reader());
        }
    }


}
