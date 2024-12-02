package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import org.apache.avro.Schema;

public record DatumReaderCacheKey(Schema writer, Schema reader) {
}