package com.example.messaging;

import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificFixed;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/** An Avro generated fixed type, in a package outside any trusted package. */
public class ExternalFixed extends SpecificFixed {

    @Override
    public Schema getSchema() {
        return Schema.createFixed("ExternalFixed", null, "com.example.messaging", 16);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.write(bytes());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        byte[] value = new byte[16];
        in.readFully(value);
        bytes(value);
    }
}
