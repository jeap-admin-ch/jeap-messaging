package ch.admin.bit.jeap.messaging.kafka.auth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(KafkaSaslAuthBeanRegistrar.class)
public class KafkaSaslAuthAutoConfiguration {
}
