package ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import java.time.Duration;

@Data
@ComponentScan
@EnableConfigurationProperties
@AutoConfiguration(value="idempotentProcessingConfigProps")
@ConfigurationProperties(prefix = "jeap.messaging.idempotent-processing")
public class IdempotentProcessingConfig {

    /**
     * Cron expression to schedule the housekeeping tasks.
     */
    public String houseKeepingSchedule = "0 0 4 * * *";

    /**
     * Duration for which idempotent processing records are kept before they get deleted by the housekeeping.
     */
    public Duration idempotentProcessingRetentionDuration = Duration.ofDays(30);

}
