package ch.admin.bit.jeap.messaging.kafka.log;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.model.MessageIdentity;
import ch.admin.bit.jeap.messaging.model.MessagePublisher;
import ch.admin.bit.jeap.messaging.model.MessageType;
import org.apache.avro.Schema;

import java.time.Instant;

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
        return new MessageIdentity() {
            @Override
            public String getId() {
                return "id";
            }

            @Override
            public String getIdempotenceId() {
                return "idempotence-id";
            }

            @Override
            public Instant getCreated() {
                return Instant.now();
            }
        };
    }

    @Override
    public MessagePublisher getPublisher() {
        return new MessagePublisher() {
            @Override
            public String getSystem() {
                return "system";
            }

            @Override
            public String getService() {
                return "system-service";
            }
        };
    }

    @Override
    public MessageType getType() {
        return new MessageType() {
            @Override
            public String getName() {
                return "TestEvent";
            }

            @Override
            public String getVersion() {
                return "1.0.0";
            }

            @Override
            public String getVariant() {
                return null;
            }
        };
    }

    @Override
    public Schema getSchema() {
        return null;
    }
}
