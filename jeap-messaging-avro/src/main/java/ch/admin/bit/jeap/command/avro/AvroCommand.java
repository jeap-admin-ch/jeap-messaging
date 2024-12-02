package ch.admin.bit.jeap.command.avro;

import ch.admin.bit.jeap.command.Command;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jeap.messaging.avro.AvroMessageIdentity;
import ch.admin.bit.jeap.messaging.avro.AvroMessagePublisher;
import ch.admin.bit.jeap.messaging.avro.AvroMessageType;
import ch.admin.bit.jeap.messaging.avro.AvroMessageUser;

public interface AvroCommand extends AvroMessage, Command {
    void setCommandVersion(String commandVersion);

    void setIdentity(AvroMessageIdentity identity);

    void setPublisher(AvroMessagePublisher publisher);

    void setType(AvroMessageType type);

    default void setUser(AvroMessageUser user) {
        throw AvroMessageBuilderException.userFieldNotDefined(this.getClass());
    }

}

