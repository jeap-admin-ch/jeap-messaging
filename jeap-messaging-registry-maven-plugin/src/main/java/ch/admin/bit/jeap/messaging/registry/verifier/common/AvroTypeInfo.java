package ch.admin.bit.jeap.messaging.registry.verifier.common;

import lombok.ToString;

@ToString
class AvroTypeInfo {
    private final String namespace;
    private final String type;
    private final String fullType;

    AvroTypeInfo(String namespace, String type) {
        this.namespace = namespace;
        this.type = type;
        fullType = namespace + "." + type;
    }

    boolean isSameType(String type) {
        return this.type.equals(type) || this.fullType.equals(type);
    }
}
