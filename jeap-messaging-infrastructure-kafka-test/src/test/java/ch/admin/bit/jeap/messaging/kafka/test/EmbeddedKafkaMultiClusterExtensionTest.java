package ch.admin.bit.jeap.messaging.kafka.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EmbeddedKafkaMultiClusterExtensionTest {

    @Test
    void returnsBootstrapServersForBothClustersAndStopsCleanly() {
        EmbeddedKafkaMultiClusterExtension extension = new EmbeddedKafkaMultiClusterExtension();

        String bootstrapServers1 = extension.getBootstrapServers1();
        String bootstrapServers2 = extension.getBootstrapServers2();

        assertNotNull(bootstrapServers1);
        assertFalse(bootstrapServers1.isBlank());
        assertNotNull(bootstrapServers2);
        assertFalse(bootstrapServers2.isBlank());
        assertNotEquals(bootstrapServers1, bootstrapServers2);

        assertEquals(bootstrapServers1, extension.getBootstrapServers1());
        assertEquals(bootstrapServers2, extension.getBootstrapServers2());

        assertDoesNotThrow(() -> extension.afterAll(mock(ExtensionContext.class)));
    }
}