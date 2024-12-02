package ch.admin.bit.jeap.messaging.idempotence.processing.jpa;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessing;
import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
@EntityScan(basePackageClasses = IdempotentProcessing.class)
@AutoConfiguration
public class IdempotentProcessingJpaConfig {

    @Bean
    @ConditionalOnMissingBean
    IdempotentProcessingRepository idempotentProcessingRepository(SpringDataJpaIdempotentProcessingRepository springDataRepository) {
        return new JpaIdempotentProcessingRepository(springDataRepository);
    }

}
