package ch.admin.bit.jeap.messaging.kafka.signature;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Optional;

@AutoConfiguration
@EnableConfigurationProperties(SignatureProducerProperties.class)
@EnableScheduling
@ComponentScan
@Import(SignatureConfiguration.SignatureMetricConfiguration.class)
public class SignatureConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "jeap.messaging.authentication.publisher", name = "signature-key")
    public SignatureService signatureService(SignatureProducerProperties signatureProducerProperties, TaskScheduler taskScheduler,
                                             Optional<SignatureMetricsService> signatureMetricsService, @Value("${spring.application.name}") String applicationName) {
        return new SignatureService(signatureProducerProperties, taskScheduler, signatureMetricsService, applicationName);
    }

    // Note: This must be a static inner class, annotated with ConditionalOnClass, to avoid Spring attempting to load the
    // MeterRegistry class because it is a bean definition method parameter.
    @Configuration
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public static class SignatureMetricConfiguration {
        @Bean
        public SignatureMetricsService signatureMetricsService(MeterRegistry meterRegistry, @Value("${spring.application.name}") String applicationName) {
            return new SignatureMetricsService(meterRegistry, applicationName);
        }
    }
}
