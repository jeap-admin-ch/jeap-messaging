package ch.admin.bit.jeap.messaging.avro.plugin.registry;

import ch.admin.bit.jeap.messaging.avro.plugin.registry.EventDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventDescriptorTest {

    @Test
    void getAllTopics() {
        String topic = "topic";
        EventDescriptor descriptorWithoutContracts = new EventDescriptor("", "", "", List.of(), topic, List.of());

        assertThat(descriptorWithoutContracts.getAllTopics()).containsOnly("topic");
    }
}