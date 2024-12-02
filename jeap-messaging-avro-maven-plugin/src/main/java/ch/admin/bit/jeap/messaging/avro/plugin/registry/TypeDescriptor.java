package ch.admin.bit.jeap.messaging.avro.plugin.registry;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public interface TypeDescriptor {
    List<TypeVersion> getVersions();

    String getDefiningSystem();

    String getName();

    String getTopic();

    Set<String> getAllTopics();

    default Set<String> allTopics(Collection<String> topics) {
        Set<String> set = new TreeSet<>();
        if (getTopic() != null) {
            set.add(getTopic());
        }
        if (topics != null) {
            set.addAll(topics);
        }
        return set;
    }
}
