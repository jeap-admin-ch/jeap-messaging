package ch.admin.bit.jeap.messaging.avro.plugin.registry;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
public class CommandDescriptor implements TypeDescriptor {
    @NonNull
    String definingSystem;
    @JsonAlias("commandName")
    @NonNull
    String name;
    String compatibilityMode;
    @NonNull
    List<TypeVersion> versions;

    /**
     * Default topic name, may be null, i.e. for shared commands
     */
    String topic;
    /**
     * Topic definitions for shared commands
     */
    List<String> topics;

    @Override
    public Set<String> getAllTopics() {
        return allTopics(topics);
    }
}
