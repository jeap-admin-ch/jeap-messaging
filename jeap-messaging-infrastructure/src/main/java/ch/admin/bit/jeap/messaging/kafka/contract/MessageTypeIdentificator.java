package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import ch.admin.bit.jeap.messaging.model.MessageType;
import lombok.Value;

@Value
class MessageTypeIdentificator {
    static MessageTypeIdentificator from(Contract contract) {
        return new MessageTypeIdentificator(contract.getMessageTypeName(), contract.getMessageTypeVersion());
    }
    static MessageTypeIdentificator from(MessageType messageType) {
        return new MessageTypeIdentificator(messageType.getName(), messageType.getVersion());
    }
    String name;
    String version;
}