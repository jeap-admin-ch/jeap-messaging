package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SubTypeResolver;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

class SubTypeResolverDeserializer extends StdDeserializer<SubTypeResolver<?, ?>> {

    public SubTypeResolverDeserializer() {
        super(SubTypeResolver.class);
    }

    @Override
    public SubTypeResolver<?, ?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return SequentialInboxConfigurationUtils.newInstance(jsonParser.getValueAsString(), SubTypeResolver.class);
    }
}
