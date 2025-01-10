package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.model.MessageType;

public interface ContractsValidator {

    void ensurePublisherContract(MessageType messageType, String topic);

    void ensureConsumerContract(String messageTypeName, String topic);

    default void ensureConsumerContract(MessageType messageType, String topic) {
        ensureConsumerContract(messageType.getName(), topic);
    }
}
