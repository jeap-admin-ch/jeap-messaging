package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.kafka.tracing.KafkaTracingConfiguration;
import ch.admin.bit.jeap.messaging.kafka.tracing.TracerBridge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test of the full Spring Boot context with embedded Kafka but without any Micrometer Tracing bridge on the
 * classpath. Asserts that {@link KafkaTracingConfiguration} stays inactive (its conditionals fail) so no
 * {@link TracerBridge} bean is wired — i.e. an application that doesn't pull in the OTel bridge boots cleanly and
 * pays no tracing cost.
 */
@SpringBootTest(properties = {
        "spring.application.name=testapp",
        "jeap.messaging.kafka.embedded=true"
})
@ContextConfiguration(classes = {TestConfig.class})
class ContextLoadsIT {

    @Autowired
    ObjectProvider<TracerBridge> tracerBridgeObjectProvider;

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void loadContextWithoutTracingBridgeSucceeds() {
        assertThat(tracerBridgeObjectProvider.getIfAvailable()).isNull();
        assertThat(applicationContext.getBeansOfType(KafkaTracingConfiguration.class)).isEmpty();
    }

}
