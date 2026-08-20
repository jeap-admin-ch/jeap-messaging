package com.example.messaging;

import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;

/** An Avro generated record would look like this, in a package outside any trusted package. */
public class ExternalRecord implements SpecificRecord {

    @Override
    public void put(int i, Object v) {
        // no fields
    }

    @Override
    public Object get(int i) {
        return null;
    }

    @Override
    public Schema getSchema() {
        return Schema.createRecord("ExternalRecord", null, "com.example.messaging", false, java.util.List.of());
    }
}
