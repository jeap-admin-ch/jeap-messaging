package ch.admin.bit.jeap.messaging.kafka.serde;

import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import lombok.RequiredArgsConstructor;
import org.apache.avro.Schema;

import java.util.Map;

/**
 * This rather special class deserialize whatever data is provided to an empty message key
 * This deserializer is intended to be used for removing the message key from a message in order
 * to prevent the misuse of complex Avro keys as data transfer containers.
 */
public class EmptyKeyDeserializer implements org.apache.kafka.common.serialization.Deserializer<Object> {

    @Override
    public void configure(Map<String, ?> map, boolean b) {
        // nothing to configure
    }

    @Override
    public AvroMessageKey deserialize(String s, byte[] bytes) {
        return new EmptyMessageKey(bytes);
    }

    @Override
    public void close() {
        // nothing to do here, there's no state at all.
    }

    @RequiredArgsConstructor
    private static class EmptyMessageKey implements AvroMessageKey {
        private final byte[] message;

        @Override
        public byte[] getSerializedMessage() {
            return message;
        }

        @Override
        public void setSerializedMessage(byte[] bytes) {
            throw new RuntimeException("Not implemented for EmptyKeyDeserializer");
        }

        @Override
        public Schema getSchema() {
            throw new RuntimeException("Not implemented for EmptyKeyDeserializer");
        }
    }
}
