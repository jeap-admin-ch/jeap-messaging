package ch.admin.bit.jeap.messaging.kafka.health;

import ch.admin.bit.jeap.messaging.kafka.KafkaConfiguration;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaAdmin;

import java.time.Duration;

@AutoConfiguration(after = KafkaConfiguration.class)
@ConditionalOnClass(HealthIndicator.class)
@ConditionalOnBean(KafkaAdmin.class)
@ConditionalOnEnabledHealthIndicator("jeap-kafka")
public class KafkaHealthConfiguration {

    @Bean
    KafkaClusterHealthIndicator jeapKafkaHealthIndicator(
            KafkaProperties kafkaProperties,
            BeanFactory beanFactory,
            @Value("${management.health.jeap-kafka.response-timeout}") Duration responseTimeout,
            @Value("${management.health.jeap-kafka.down-after}") Duration downAfter) {
        return new KafkaClusterHealthIndicator(kafkaProperties, beanFactory, responseTimeout, downAfter);
    }
}
