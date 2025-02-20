package ch.admin.bit.jeap.messaging.transactionaloutbox.metrics;

import ch.admin.bit.jeap.messaging.kafka.metrics.KafkaMessagingMetrics;
import ch.admin.bit.jeap.messaging.kafka.signature.publisher.SignaturePublisherProperties;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessageRepository;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.FailedMessageRepository;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxMetrics;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@ConditionalOnClass(MeterRegistry.class)
@EnableAspectJAutoProxy
@AutoConfiguration
public class OutboxMetricsConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    OutboxMetrics outboxMetrics(MeterRegistry meterRegistry, DeferredMessageRepository deferredMessageRepository, FailedMessageRepository failedMessageRepository,
                                KafkaMessagingMetrics kafkaMessagingMetrics, SignaturePublisherProperties signaturePublisherProperties) {
        return new MicrometerOutboxMetrics(meterRegistry, deferredMessageRepository, failedMessageRepository, kafkaMessagingMetrics, signaturePublisherProperties, applicationName);
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }

}
