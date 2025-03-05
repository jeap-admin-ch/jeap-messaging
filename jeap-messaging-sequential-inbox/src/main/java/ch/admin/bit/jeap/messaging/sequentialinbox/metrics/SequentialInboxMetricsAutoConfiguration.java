package ch.admin.bit.jeap.messaging.sequentialinbox.metrics;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequentialInboxConfiguration;
import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.MessageRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration(after = {MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@EnableScheduling
@ConditionalOnBean(MeterRegistry.class)
class SequentialInboxMetricsAutoConfiguration {

    @Bean
    SequentialInboxMetrics sequentialInboxMetrics(SequentialInboxPersistenceMetrics persistenceMetrics,
                                                  MeterRegistry meterRegistry,
                                                  SequentialInboxConfiguration configuration) {
        return new SequentialInboxMetrics(meterRegistry, configuration, persistenceMetrics);
    }

    @Bean
    SequentialInboxPersistenceMetrics sequentialInboxPersistenceMetrics(MessageRepository messageRepository) {
        return new SequentialInboxPersistenceMetrics(messageRepository);
    }
}
