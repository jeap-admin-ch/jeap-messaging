package ch.admin.bit.jeap.messaging.kafka.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass({MeterRegistry.class})
public class KafkaMetricsConfiguration {

    @Bean
    KafkaMessagingMetrics kafkaMetrics(MeterRegistry meterRegistry){
        return new KafkaMeterRegistryMetrics(meterRegistry);
    }

}
