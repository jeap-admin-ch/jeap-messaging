package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.transactionaloutbox.spring.DeferredMessageSenderProvider;
import ch.admin.bit.jeap.messaging.transactionaloutbox.spring.OutboxBeanRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.util.Map;

@EnableConfigurationProperties
@AutoConfiguration
@ComponentScan
@Import(OutboxBeanRegistrar.class)
public class OutboxConfig {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    DeferredMessageSenderProvider deferredMessageSenderProvider(KafkaProperties kafkaProperties,
                                                                Map<String, DeferredMessageSender> deferredMessageSendersByBeanName) {
        String defaultClusterName = kafkaProperties.getDefaultClusterName();
        String defaultProducerClusterName = kafkaProperties.getDefaultProducerClusterName();
        return new DeferredMessageSenderProvider(defaultClusterName, defaultProducerClusterName, deferredMessageSendersByBeanName);
    }
}
