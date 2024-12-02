package ch.admin.bit.jeap.messaging.kafka.test.integration.observation;

import ch.admin.bit.jeap.messaging.kafka.tracing.KafkaTracingConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(KafkaTracingConfiguration.class)
public class TracingTestConfiguration {
}
