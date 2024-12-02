package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.model.MessageType;

public interface ContractsValidator {

    void ensurePublisherContract(MessageType messageType, String topic);

    void ensureConsumerContract(MessageType messageType, String topic);

}
