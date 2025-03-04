package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;

public interface MessageFilter<T extends AvroMessage> {
    /**
     * @return true = message should be sequenced, false = message should not be sequenced and processed immediately
     */
    boolean shouldSequence(T message);
}
