package ch.admin.bit.jeap.messaging.kafka.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.stereotype.Component;

/**
 * @deprecated Use {@link KafkaIntegrationTestBase} instead
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Deprecated(forRemoval = true)
public class WaitForKafkaHelper {
    private final KafkaListenerEndpointRegistry registry;

    @SuppressWarnings("unused")
    public void waitForKafkaListener() {
        registry.getListenerContainers()
                .forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }
}
