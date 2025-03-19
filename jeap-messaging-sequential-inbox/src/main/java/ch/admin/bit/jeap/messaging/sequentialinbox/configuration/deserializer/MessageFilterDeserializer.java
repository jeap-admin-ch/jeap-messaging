package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.MessageFilter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

class MessageFilterDeserializer extends StdDeserializer<MessageFilter<?>> {

    public MessageFilterDeserializer() {
        super(MessageFilter.class);
    }

    @Override
    public MessageFilter<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return SequentialInboxConfigurationUtils.newInstance(jsonParser.getValueAsString(), MessageFilter.class);
    }
}
