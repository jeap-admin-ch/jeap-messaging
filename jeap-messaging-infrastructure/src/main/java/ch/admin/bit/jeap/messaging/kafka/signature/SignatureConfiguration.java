package ch.admin.bit.jeap.messaging.kafka.signature;

import ch.admin.bit.jeap.messaging.kafka.signature.publisher.DefaultSignatureService;
import ch.admin.bit.jeap.messaging.kafka.signature.publisher.SignaturePublisherProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.subscriber.*;
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
@EnableConfigurationProperties({SignaturePublisherProperties.class, SignatureSubscriberProperties.class})
@EnableScheduling
@ComponentScan
@Import(SignatureConfiguration.SignatureMetricConfiguration.class)
public class SignatureConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "jeap.messaging.authentication.publisher", name = "signature-key")
    public SignaturePublisherCheck signaturePublisherCheck(SignaturePublisherProperties signaturePublisherProperties, SignatureVerifier signatureVerifier) {
        return new SignaturePublisherCheck(signaturePublisherProperties, signatureVerifier);
    }

    @Bean
    @ConditionalOnProperty(prefix = "jeap.messaging.authentication.publisher", name = "signature-key")
    public DefaultSignatureService signatureService(SignaturePublisherProperties signaturePublisherProperties, TaskScheduler taskScheduler,
                                             Optional<SignatureMetricsService> signatureMetricsService, @Value("${spring.application.name}") String applicationName) {
        return new DefaultSignatureService(signaturePublisherProperties, taskScheduler, signatureMetricsService, applicationName);
    }

    @Bean
    @ConditionalOnProperty(prefix = "jeap.messaging.authentication.subscriber", name = "require-signature")
    public DefaultSignatureAuthenticityService signatureAuthenticityService(SubscriberValidationPropertiesContainer validationPropertiesContainer,
                                                                     CertificateAndSignatureVerifier certificateAndSignatureVerifier,
                                                                            SubscriberCertificatesContainer subscriberCertificatesContainer,
                                                                     Optional<SignatureMetricsService> signatureMetricsService) {
        return new DefaultSignatureAuthenticityService(validationPropertiesContainer, certificateAndSignatureVerifier, subscriberCertificatesContainer, signatureMetricsService);
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
