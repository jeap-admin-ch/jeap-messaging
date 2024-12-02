package ch.admin.bit.jeap.messaging.avro;

import ch.admin.bit.jeap.kafka.SerializedMessageReceiver;
import org.apache.avro.generic.GenericContainer;

/**
 * Base interface to be supported by all Avro message key types.
 * Currently only a marker interface.
 */
public interface AvroMessageKey extends GenericContainer, SerializedMessageReceiver, SerializedMessageHolder {
}
