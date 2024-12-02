package ch.admin.bit.jeap.messaging.kafka.properties.cluster;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GluePropertiesTest {

    @Test
    void validateProperties() {
        GlueProperties props = new GlueProperties();

        props.setRegion("eu-test-1");
        props.setRegistryName("test-registry");

        assertThatCode(() -> props.validateProperties(false))
                .doesNotThrowAnyException();
    }

    @Test
    void validateProperties_shouldThrow_whenRegionIsMissing() {
        GlueProperties props = new GlueProperties();

        props.setRegistryName("test-registry");

        assertThatThrownBy(() -> props.validateProperties(false))
                .hasMessageContaining("region");
    }

    @Test
    void validateProperties_shouldThrow_whenConfluentIsAlsoActive() {
        GlueProperties props = new GlueProperties();

        props.setRegistryName("test-registry");

        assertThatThrownBy(() -> props.validateProperties(true))
                .hasMessageContaining("Confluent schema registry and Glue schema registry cannot be active at the same time");
    }
}