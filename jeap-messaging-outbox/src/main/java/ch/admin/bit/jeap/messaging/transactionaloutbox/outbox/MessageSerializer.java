package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.model.Message;

public interface MessageSerializer {

    byte[] serializeMessage(Message message, String topic);

    byte[] serializeKey(Object key, String topic);

}
