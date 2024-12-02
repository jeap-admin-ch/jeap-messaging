package ch.admin.bit.jeap.messaging.transactionaloutbox.jpa;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@EnableJpaRepositories
@EntityScan(basePackageClasses = DeferredMessage.class)
@ComponentScan
@AutoConfiguration
public class OutboxJpaConfig {
}
