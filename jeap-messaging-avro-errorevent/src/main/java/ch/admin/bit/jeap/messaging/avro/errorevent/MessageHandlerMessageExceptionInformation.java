package ch.admin.bit.jeap.messaging.avro.errorevent;

import ch.admin.bit.jeap.messaging.model.Message;

public interface MessageHandlerMessageExceptionInformation extends MessageHandlerExceptionInformation {

    Message getMessagingMessage();
}
