package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport;

import ch.admin.bit.jeap.messaging.transactionaloutbox.config.TransactionalOutboxConfigurationProperties;
import ch.admin.bit.jeap.messaging.transactionaloutbox.jpa.OutboxJpaConfig;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxConfig;
import ch.admin.bit.jeap.messaging.transactionaloutbox.transaction.OutboxTransactionConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({OutboxConfig.class,
        OutboxJpaConfig.class,
        OutboxTransactionConfig.class,
        TransactionalOutboxConfigurationProperties.class,
        OutboxKafkaMockConfig.class,})
public class OutboxMockKafkaNoSchedulingTestConfig {
}
