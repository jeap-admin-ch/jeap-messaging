package ch.admin.bit.jeap.messaging.transactionaloutbox.scheduling;

import ch.admin.bit.jeap.messaging.transactionaloutbox.metrics.OutboxMetricsConfig;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@ConditionalOnProperty(name ="jeap.messaging.transactional-outbox.scheduled-relay-enabled", matchIfMissing = true)
@AutoConfiguration(after = OutboxMetricsConfig.class)
@ComponentScan
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "5m")
public class OutboxSchedulingConfig {

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

    @ConditionalOnClass(MeterRegistry.class)
    @Bean
    public OutboxMetricsUpdateScheduler outboxMetricsUpdateScheduler(OutboxMetrics outboxMetrics) {
        return new OutboxMetricsUpdateScheduler(outboxMetrics);
    }
}
