package ch.admin.bit.jeap.messaging.avro.plugin.registry.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.NonNull;
import lombok.Value;

@Value
public class TypeReference {
    @NonNull
    @JsonAlias("publishingSystem")
    String definingSystem;
    @NonNull
    String name;
    @NonNull
    String version;
}
