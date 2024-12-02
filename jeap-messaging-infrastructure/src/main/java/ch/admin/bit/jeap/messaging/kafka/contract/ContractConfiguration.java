package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(ContractConfiguration.ContractMetricConfiguration.class)
public class ContractConfiguration {

    @Bean
    @ConditionalOnMissingBean(ContractsProvider.class)
    public ContractsProvider defaultContractsProvider() {
        return new DefaultContractsProvider();
    }

    @Bean
    @ConditionalOnMissingBean(ContractsValidator.class)
    public ContractsValidator defaultContractsValidator(@Value("${spring.application.name}") String appName, ContractsProvider contractsProvider) {
        return new DefaultContractsValidator(appName, contractsProvider);
    }

    // Note: This must be a static inner class, annotated with ConditionalOnClass, to avoid Spring attempting to load the
    // MeterRegistry class because it is a bean definition method parameter.
    @Configuration
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public static class ContractMetricConfiguration {
        @Bean
        public ContractMetric contractMetric(MeterRegistry meterRegistry, KafkaProperties kafkaProperties, ContractsProvider contractsProvider) {
            return new ContractMetric(meterRegistry, kafkaProperties, contractsProvider);
        }
    }
}
