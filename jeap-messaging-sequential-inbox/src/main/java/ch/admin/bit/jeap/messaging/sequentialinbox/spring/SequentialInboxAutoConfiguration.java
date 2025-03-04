package ch.admin.bit.jeap.messaging.sequentialinbox.spring;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer.SequentialInboxConfigurationLoader;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequentialInboxConfiguration;
import ch.admin.bit.jeap.messaging.sequentialinbox.inbox.SequentialInboxService;
import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.MessageRepository;
import ch.admin.bit.jeap.messaging.sequentialinbox.kafka.KafkaSequentialInboxMessageListener;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequenceInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan
@ComponentScan(basePackageClasses = {SequentialInboxService.class, MessageRepository.class, KafkaSequentialInboxMessageListener.class})
@EnableJpaRepositories(basePackageClasses = {MessageRepository.class})
@EntityScan(basePackageClasses = SequenceInstance.class)
@ConditionalOnProperty(prefix = "jeap.messaging.sequential-inbox", name = "enabled", havingValue = "true", matchIfMissing = true)
class SequentialInboxAutoConfiguration {

    private static final String DEFAULT_CONFIG_LOCATION = "classpath:/messaging/jeap-sequential-inbox.yml";

    @Bean
    SequentialInboxConfiguration sequentialInboxConfiguration(
            @Value("${jeap.messaging.sequential-inbox.config-location:" + DEFAULT_CONFIG_LOCATION + "}") String configLocation) {
        SequentialInboxConfigurationLoader loader = new SequentialInboxConfigurationLoader(configLocation);
        return loader.loadSequenceDeclaration();
    }
}
