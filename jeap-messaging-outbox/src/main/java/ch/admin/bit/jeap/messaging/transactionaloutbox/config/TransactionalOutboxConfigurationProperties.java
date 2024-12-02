package ch.admin.bit.jeap.messaging.transactionaloutbox.config;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutboxConfiguration;
import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@AutoConfiguration(value="txOutboxConfigProps")
@ConfigurationProperties(prefix = "jeap.messaging.transactional-outbox")
public class TransactionalOutboxConfigurationProperties implements TransactionalOutboxConfiguration {

    public Duration pollDelay = Duration.ofSeconds(2);

    public Duration continuousRelayTimeout = Duration.ofMinutes(5);

    public int messageRelayBatchSize = 5;

    public Duration messageSendImmediatelyTimeout = Duration.ofSeconds(15);

    public Duration messageSendImmediatelyMaxBlockTime = Duration.ofSeconds(5);

    public Duration messageSendScheduledTimeout = Duration.ofSeconds(60);

    public Duration messageSendScheduledMaxBlockTime = Duration.ofSeconds(15);

    public boolean scheduledRelayEnabled = true;

    public String houseKeepingSchedule = "0 0 3 * * *";

    public Duration sentMessageRetentionDuration = Duration.ofDays(2);

    public Duration unsentMessageRetentionDuration = Duration.ofDays(30);

    public Duration metricsUpdateInterval = Duration.ofSeconds(10);

}

