package ch.admin.bit.jeap.messaging.annotations;

import ch.admin.bit.jeap.messaging.avro.MessageTypeMetadata;
import org.apache.avro.Schema;
import org.apache.avro.specific.AvroGenerated;

/**
 * An avro generated message type as the jeap-messaging-avro-maven-plugin would generate it, but compiled in the same
 * compilation run as the contract annotation referencing it. In that situation the class is not available as bytecode
 * while the annotation processor runs, so the schema has to be read from the source tree.
 * <p>
 * The SCHEMA$ initializer is split into several string literals on purpose, as avro does that for larger schemas.
 */
@SuppressWarnings("unused")
@AvroGenerated
public class SameCompilationUnitTestEvent {

    public static final Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"SameCompilationUnitTestEvent\",",
            "\"namespace\":\"ch.admin.bit.jeap.messaging.annotations\",\"fields\":[{\"name\":\"domainEventVersion\",\"type\":\"string\"}]}");

    public static Schema getClassSchema() {
        return SCHEMA$;
    }

    public static class TypeRef implements MessageTypeMetadata {
        public static final String REGISTRY_URL = "https://testhost/projects/TEST/repos/test-message-type-registry/";
        public static final String REGISTRY_BRANCH = "master";
        public static final String REGISTRY_COMMIT = "HEAD";
        public static final String COMPATIBILITY_MODE = "BACKWARD";
        public static final String MESSAGE_TYPE_VERSION = "1.0.0";
        public static final String MESSAGE_TYPE_NAME = "SameCompilationUnitTestEvent";
        public static final String SYSTEM_NAME = "JME";
        public static final String DEFAULT_TOPIC = "same-compilation-unit-topic";
    }
}
