package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;

@Data
@AutoConfiguration
@ComponentScan
@EnableAspectJAutoProxy
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "jeap.messaging.idempotent-message-handler")
public class IdempotentMessageHandlerConfig {

    // Lowest precedence puts this advice nearest to the method execution, i.e. after advices having a higher priority.
    private int adviceOrder = Ordered.LOWEST_PRECEDENCE;

}
