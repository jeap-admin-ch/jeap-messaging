package ch.admin.bit.jeap.messaging.sequentialinbox.housekeeping;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@AutoConfiguration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "5m")
@ConditionalOnProperty(name = "jeap.messaging.sequential-inbox.housekeeping.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan
class SequentialInboxHousekeepingAutoconfiguration {

    @ConditionalOnMissingBean
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }

}
