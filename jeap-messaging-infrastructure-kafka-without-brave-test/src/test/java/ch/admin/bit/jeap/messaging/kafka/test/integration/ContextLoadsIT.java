package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.kafka.tracing.TracerBridge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.application.name=testapp",
        "jeap.messaging.kafka.embedded=true"
})
@ContextConfiguration(classes = {TestConfig.class})
public class ContextLoadsIT {

    @Autowired
    ObjectProvider<TracerBridge> tracerBridgeObjectProvider;

    @Test
    void contextLoads() {
        assertThat(tracerBridgeObjectProvider.getIfAvailable())
                .isNull();
    }

}
