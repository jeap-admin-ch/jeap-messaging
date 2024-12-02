package ch.admin.bit.jeap.messaging.kafka.reactive;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(JeapReactiveKafkaBeanRegistrar.class)
public class ReactiveKafkaAutoConfig {
}
