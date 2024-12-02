package ch.admin.bit.jeap.messaging.avro.plugin.registry;

import ch.admin.bit.jeap.messaging.avro.plugin.registry.CommandDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandDescriptorTest {

    @Test
    void getAllTopics() {
        String topic = "topic";
        CommandDescriptor descriptorWithoutContracts = new CommandDescriptor("", "", "", List.of(), topic, List.of());

        assertThat(descriptorWithoutContracts.getAllTopics()).containsOnly("topic");
    }
}
