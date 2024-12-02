package ch.admin.bit.jeap.messaging.avro.plugin.validator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A record collection is a normalized collection of records from a set of protocols and schemas.
 * Schemas in Avro can include nested types, which makes the validation quite complex. Normalized here means
 * that we collect all those nested types and provide them in a single list.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecordCollection {
    private final List<Schema> records = new LinkedList<>();

    public static RecordCollection of(Schema schema) {
        RecordCollection ret = new RecordCollection();
        ret.collectRecords(schema);
        return ret;
    }

    public static RecordCollection of(Protocol protocols) {
        RecordCollection ret = new RecordCollection();
        protocols.getTypes().forEach(ret::collectRecords);
        return ret;
    }

    public List<Schema> getRecords() {
        return Collections.unmodifiableList(records);
    }

    private void collectRecords(Schema schema) {
        if (records.contains(schema))
            return;
        switch (schema.getType()) {
            case RECORD:
                records.add(schema);
                for (Schema.Field field : schema.getFields())
                    collectRecords(field.schema());
                break;
            case MAP:
                collectRecords(schema.getValueType());
                break;
            case ARRAY:
                collectRecords(schema.getElementType());
                break;
            case UNION:
                for (Schema s : schema.getTypes())
                    collectRecords(s);
                break;
            default:
                break;
        }
    }
}
