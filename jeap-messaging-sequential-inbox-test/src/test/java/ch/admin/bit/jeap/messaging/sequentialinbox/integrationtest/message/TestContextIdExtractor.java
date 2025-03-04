package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.ContextIdExtractor;

public class TestContextIdExtractor implements ContextIdExtractor<AvroMessage> {

    @Override
    public String extractContextId(AvroMessage message) {
        return message.getOptionalProcessId().orElse(null);
    }
}
