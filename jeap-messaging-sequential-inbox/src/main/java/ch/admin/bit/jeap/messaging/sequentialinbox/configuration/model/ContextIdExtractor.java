package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;

public interface ContextIdExtractor<T extends AvroMessage> {
    String extractContextId(T message);
}