package ch.admin.bit.jeap.kafka;

/**
 * Avro classes implementing this interface will get the original message set as well.
 */
public interface SerializedMessageReceiver {
    void setSerializedMessage(byte[] message);
}
