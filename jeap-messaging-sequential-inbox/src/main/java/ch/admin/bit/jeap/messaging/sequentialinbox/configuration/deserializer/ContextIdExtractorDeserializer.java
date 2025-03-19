package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.ContextIdExtractor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

class ContextIdExtractorDeserializer extends StdDeserializer<ContextIdExtractor<?>> {

    public ContextIdExtractorDeserializer() {
        super(ContextIdExtractor.class);
    }

    @Override
    public ContextIdExtractor<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return SequentialInboxConfigurationUtils.newInstance(jsonParser.getValueAsString(), ContextIdExtractor.class);
    }
}
