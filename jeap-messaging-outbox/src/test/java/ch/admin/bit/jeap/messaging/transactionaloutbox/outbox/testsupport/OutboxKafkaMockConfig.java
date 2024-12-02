package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.MessageSerializer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class OutboxKafkaMockConfig {

    @Bean("kafkaOutboxAvroMessageSerializer")
    MessageSerializer messageSerializer() {
        return new ToStringMessageSerializer();
    }

    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
