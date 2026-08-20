package com.example.messaging;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericEnumSymbol;

/** An Avro generated enum, in a package outside any trusted package. */
public enum ExternalEnum implements GenericEnumSymbol<ExternalEnum> {
    A;

    @Override
    public Schema getSchema() {
        return Schema.createEnum("ExternalEnum", null, "com.example.messaging", java.util.List.of("A"));
    }
}
