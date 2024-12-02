package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.MessageSerializer;

import java.nio.charset.StandardCharsets;

public class ToStringMessageSerializer implements MessageSerializer {

    @Override
    public byte[] serializeMessage(Message message, String topic) {
        return message.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] serializeKey(Object key, String topic) {
        return key.toString().getBytes(StandardCharsets.UTF_8);
    }

}
