package ch.admin.bit.jeap.messaging.kafka.properties;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaPropertiesConfiguration {
}
