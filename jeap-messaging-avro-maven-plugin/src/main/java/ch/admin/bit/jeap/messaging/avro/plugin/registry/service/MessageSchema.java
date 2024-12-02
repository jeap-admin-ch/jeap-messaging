package ch.admin.bit.jeap.messaging.avro.plugin.registry.service;

import ch.admin.bit.jeap.messaging.avro.plugin.registry.metadata.MessageTypeMetadata;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.io.File;
import java.util.Map;
import java.util.Optional;

@Builder
@Value
public class MessageSchema {
    @NonNull
    File schema;
    @NonNull
    Map<String, File> importPath;

    TypeReference typeReference;

    MessageTypeMetadata messageTypeMetadata;

    public Optional<MessageTypeMetadata> getMessageTypeMetadata() {
        return Optional.ofNullable(messageTypeMetadata);
    }

    public Optional<TypeReference> getTypeReference() {
        return Optional.ofNullable(typeReference);
    }
}
