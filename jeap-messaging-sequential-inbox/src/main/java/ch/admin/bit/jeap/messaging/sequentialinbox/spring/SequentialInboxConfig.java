package ch.admin.bit.jeap.messaging.sequentialinbox.spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@EnableConfigurationProperties
@AutoConfiguration
@ComponentScan
public class SequentialInboxConfig {
}
