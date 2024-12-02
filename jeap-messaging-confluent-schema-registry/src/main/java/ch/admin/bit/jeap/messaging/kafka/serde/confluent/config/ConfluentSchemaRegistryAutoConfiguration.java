package ch.admin.bit.jeap.messaging.kafka.serde.confluent.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(ConfluentSchemaRegistryBeanRegistrar.class)
public class ConfluentSchemaRegistryAutoConfiguration {
}
