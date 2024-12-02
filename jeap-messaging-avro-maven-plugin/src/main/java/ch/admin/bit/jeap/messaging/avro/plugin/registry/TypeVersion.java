package ch.admin.bit.jeap.messaging.avro.plugin.registry;

import lombok.NonNull;
import lombok.Value;

@Value
public class TypeVersion {
    @NonNull
    String version;
    @NonNull
    String valueSchema;
    String keySchema;
    String compatibilityMode;
}
