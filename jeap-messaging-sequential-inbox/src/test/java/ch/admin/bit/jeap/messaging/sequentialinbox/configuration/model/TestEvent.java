package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.model.MessageIdentity;
import ch.admin.bit.jeap.messaging.model.MessagePublisher;
import ch.admin.bit.jeap.messaging.model.MessageType;
import org.apache.avro.Schema;

public class TestEvent implements AvroMessage {
    @Override
    public void setSerializedMessage(byte[] message) {

    }

    @Override
    public byte[] getSerializedMessage() {
        return new byte[0];
    }

    @Override
    public MessageIdentity getIdentity() {
        return null;
    }

    @Override
    public MessagePublisher getPublisher() {
        return null;
    }

    @Override
    public MessageType getType() {
        return null;
    }

    @Override
    public Schema getSchema() {
        return null;
    }
}
