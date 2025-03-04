package ch.admin.bit.jeap.messaging.sequentialinbox.housekeeping;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableScheduling
@ConditionalOnProperty(name = "jeap.messaging.sequential-inbox.housekeeping.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan
class SequentialInboxHousekeepingAutoconfiguration {
    // This class only enables scheduling and component scanning, no additional configuration needed
}
