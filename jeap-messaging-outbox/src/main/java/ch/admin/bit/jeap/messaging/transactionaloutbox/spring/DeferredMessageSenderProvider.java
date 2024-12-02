package ch.admin.bit.jeap.messaging.transactionaloutbox.spring;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessage;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessageSender;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.StringUtils.hasText;

/**
 * Provides {@link DeferredMessageSender} instances by kafka cluster name
 */
@Slf4j
public class DeferredMessageSenderProvider {

    private final OutboxBeanNames outboxBeanNames;
    private final Map<String, DeferredMessageSender> deferredMessageSendersByBeanName;
    private final String defaultProducerClusterName;
    private final DeferredMessageSender defaultProducerClusterDeferredMessageSender;

    public DeferredMessageSenderProvider(String defaultClusterName, String defaultProducerClusterName, Map<String, DeferredMessageSender> deferredMessageSendersByBeanName) {
        this.outboxBeanNames = new OutboxBeanNames(defaultClusterName);
        this.deferredMessageSendersByBeanName = deferredMessageSendersByBeanName;
        this.defaultProducerClusterName = defaultProducerClusterName;
        this.defaultProducerClusterDeferredMessageSender = requireNonNull(getDeferredMessageSenderForCluster(defaultProducerClusterName),
                "Expecting a deferred message sender to exist for the default producer cluster '%s'.".formatted(defaultProducerClusterName));
    }

    public DeferredMessageSender getDeferredMessageSenderForCluster(DeferredMessage message) {
        String clusterName = hasText(message.getClusterName()) ? message.getClusterName() : defaultProducerClusterName;
        DeferredMessageSender deferredMessageSender = getDeferredMessageSenderForCluster(clusterName);
        if (deferredMessageSender == null) {
            log.debug("Unknown cluster name '{}' found in the deferred message {}. Using the default producer cluster '{}' instead.",
                    clusterName, message, defaultProducerClusterName);
            deferredMessageSender = defaultProducerClusterDeferredMessageSender;
        }
        return deferredMessageSender;
    }

    private DeferredMessageSender getDeferredMessageSenderForCluster(String clusterName) {
        String beanName = outboxBeanNames.getDeferredMessageSenderBeanName(clusterName);
        return deferredMessageSendersByBeanName.get(beanName);
    }

}
