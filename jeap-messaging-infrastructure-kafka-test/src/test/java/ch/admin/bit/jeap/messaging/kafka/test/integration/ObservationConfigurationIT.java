package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {TestConfig.class}, properties = {
        "spring.application.name=jme-test-observation-service"})
@Slf4j
@DirtiesContext
class ObservationConfigurationIT extends KafkaIntegrationTestBase {
    @Autowired
    private ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Test
    void observationIsEnabled() throws NoSuchFieldException, IllegalAccessException {
        assertTrue(kafkaListenerContainerFactory.getContainerProperties().isObservationEnabled());
        Field observationEnabled = kafkaTemplate.getClass().getDeclaredField("observationEnabled");
        observationEnabled.setAccessible(true);
        assertTrue(observationEnabled.getBoolean(kafkaTemplate));
        observationEnabled.setAccessible(false);
    }

}
