package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.kafka.KafkaConfiguration;
import ch.admin.bit.jeap.messaging.kafka.tracing.TracerBridge;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.config.AbstractKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
class KafkaConsumerConfigurationIT {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KafkaConfiguration.class));

    @Test
    void loadContextWithoutBrave() {
        this.contextRunner
                .withPropertyValues(
                        "spring.application.name=jme-messaging-subscriber-service",
                        "jeap.messaging.kafka.embedded=true")
                .withUserConfiguration(TestConfig.class)
                .run((context) -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(TracerBridge.class);
                    assertThat(context).hasSingleBean(KafkaListenerContainerFactory.class);

                    assertObservationDisabledInContainerFactory(context);
                    assertObservationDisabledInKafkaTemplate(context);
                });
    }

    private static void assertObservationDisabledInKafkaTemplate(AssertableApplicationContext context) throws NoSuchFieldException, IllegalAccessException {
        assertThat(context).hasSingleBean(KafkaTemplate.class);
        KafkaTemplate kafkaTemplate = context.getBean(KafkaTemplate.class);
        Field observationEnabled = kafkaTemplate.getClass().getDeclaredField("observationEnabled");
        observationEnabled.setAccessible(true);
        assertFalse(observationEnabled.getBoolean(kafkaTemplate));
    }

    private static void assertObservationDisabledInContainerFactory(AssertableApplicationContext context) {
        AbstractKafkaListenerContainerFactory kafkaListenerContainerFactory = context.getBean(AbstractKafkaListenerContainerFactory.class);
        assertThat(kafkaListenerContainerFactory.getContainerProperties().isObservationEnabled()).isFalse();
    }

}
