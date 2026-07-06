package ch.admin.bit.jeap.messaging.idempotence.processing.jpa;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessing;
import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

@Slf4j
@EnableJpaRepositories
@EntityScan(basePackageClasses = IdempotentProcessing.class)
@AutoConfiguration
public class IdempotentProcessingJpaConfig {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    @ConditionalOnMissingBean
    IdempotentProcessingRepository idempotentProcessingRepository(SpringDataJpaIdempotentProcessingRepository springDataRepository,
                                                                  DataSource dataSource,
                                                                  @Value("${jeap.messaging.idempotent-processing.insert-mode:auto}") String insertModeValue) {
        IdempotentProcessingInsertMode insertMode = IdempotentProcessingInsertMode.fromPropertyValue(insertModeValue);
        boolean onConflictDoNothingInsert = switch (insertMode) {
            case AUTO -> isPostgreSql(dataSource);
            case ON_CONFLICT_DO_NOTHING -> true;
            case WHERE_NOT_EXISTS -> false;
        };
        log.info("Creating idempotent processing records using 'INSERT ... {}' (insert mode {}).",
                onConflictDoNothingInsert ? "ON CONFLICT DO NOTHING" : "WHERE NOT EXISTS", insertMode);
        return new JpaIdempotentProcessingRepository(springDataRepository, onConflictDoNothingInsert);
    }

    private static boolean isPostgreSql(DataSource dataSource) {
        try {
            String productName = JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName);
            return "PostgreSQL".equalsIgnoreCase(productName);
        } catch (MetaDataAccessException e) {
            log.warn("Unable to determine the database product name. Falling back to the portable idempotent processing insert.", e);
            return false;
        }
    }

}
