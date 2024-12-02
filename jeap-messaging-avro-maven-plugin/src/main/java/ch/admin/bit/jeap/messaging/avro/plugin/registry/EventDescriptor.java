package ch.admin.bit.jeap.messaging.avro.plugin.registry;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
public class EventDescriptor implements TypeDescriptor {
    @JsonAlias("publishingSystem")
    String definingSystem;
    @JsonAlias("eventName")
    @NonNull
    String name;
    String compatibilityMode;
    @NonNull
    List<TypeVersion> versions;
    /**
     * Default topic name, may be null, i.e. for shared events
     */
    String topic;
    /**
     * Topic definitions for shared events
     */
    List<String> topics;

    @Override
    public Set<String> getAllTopics() {
        return allTopics(topics);
    }
}
