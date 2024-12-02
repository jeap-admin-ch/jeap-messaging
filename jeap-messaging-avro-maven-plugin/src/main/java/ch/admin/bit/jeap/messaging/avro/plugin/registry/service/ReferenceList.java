package ch.admin.bit.jeap.messaging.avro.plugin.registry.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class ReferenceList {
    @NonNull
    @JsonAlias("stashUrl")
    String repoUrl;
    @NonNull
    String branch;
    @NonNull
    @JsonAlias("events")
    List<TypeReference> types;
}
