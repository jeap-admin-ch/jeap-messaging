package ch.admin.bit.jeap.messaging.avro.plugin.validator;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.LinkedList;

class RecordCollectionTest {
    @Test
    void single() {
        Schema schema = SchemaBuilder.record("schema").fields().endRecord();

        RecordCollection target = RecordCollection.of(schema);

        Assertions.assertEquals(1, target.getRecords().size());
        Assertions.assertTrue(target.getRecords().contains(schema));
    }

    @Test
    void subSchema() {
        Schema subSchema = SchemaBuilder.record("sub").fields().endRecord();
        Schema schema = SchemaBuilder.record("schema").fields()
                .name("field")
                .type(subSchema)
                .noDefault()
                .endRecord();

        RecordCollection target = RecordCollection.of(schema);

        Assertions.assertEquals(2, target.getRecords().size());
        Assertions.assertTrue(target.getRecords().contains(subSchema));
        Assertions.assertTrue(target.getRecords().contains(schema));
    }


    @Test
    void schemaTwice() {
        Schema subSchema = SchemaBuilder.record("sub").fields().endRecord();
        Schema schema = SchemaBuilder.record("schema").fields()
                .name("field").type(subSchema).noDefault()
                .name("field2").type(subSchema).noDefault()
                .endRecord();

        RecordCollection target = RecordCollection.of(schema);

        //Still only 2, sub is not taken twice...
        Assertions.assertEquals(2, target.getRecords().size());
    }

    @Test
    void protocol() {
        Schema schema1 = SchemaBuilder.record("schema1").fields().endRecord();
        Schema schema2 = SchemaBuilder.record("schema2").fields().endRecord();
        Collection<Schema> schemas = new LinkedList<>();
        schemas.add(schema1);
        schemas.add(schema2);
        Protocol protocol = new Protocol("protocol", "documentation", "ch.admin");
        protocol.setTypes(schemas);

        RecordCollection target = RecordCollection.of(protocol);

        Assertions.assertEquals(2, target.getRecords().size());
        Assertions.assertTrue(target.getRecords().contains(schema1));
        Assertions.assertTrue(target.getRecords().contains(schema2));
    }
}
