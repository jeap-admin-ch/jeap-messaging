package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.contract.DefaultContractsProvider;
import ch.admin.bit.jeap.messaging.kafka.contract.DefaultContractsValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContractsValidationConfiguration {
    @Bean
    ContractsValidator contractsValidator(@Value("${spring.application.name}") String appName) {
        return new DefaultContractsValidator(appName, new DefaultContractsProvider());
    }
}