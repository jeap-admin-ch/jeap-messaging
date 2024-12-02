package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JeapCustomDatumReaderFactory {

    @SuppressWarnings("unchecked")
    public static DatumReader<Object> from(Schema writerSchema, Schema reader)
        throws InstantiationException, IllegalAccessException {
        Class<SpecificRecord> readerClass = SpecificData.get().getClass(reader);
        Schema readerSchema = readerClass.newInstance().getSchema();
        return new SpecificDatumReader<>(writerSchema, readerSchema);
    }
}
