package ch.admin.bit.jeap.messaging.avro.plugin.registry.metadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageTypeMetadataProviderTest {

    @Test
    void generateConstantNameForTopicName() {
        assertEquals("TOPIC_FOO_BAR_V123", MessageTypeMetadataProvider.generateConstantNameForTopicName("foo-bar-v123"));
        assertEquals("TOPIC_FOO_BAR_V123", MessageTypeMetadataProvider.generateConstantNameForTopicName("foo!bar_v123"));
    }
}