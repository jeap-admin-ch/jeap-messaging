package ch.admin.bit.jeap.messaging.transactionaloutbox.spring;

import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;

public class OutboxBeanNames {
    private final JeapKafkaBeanNames jeapKafkaBeanNames;

    OutboxBeanNames(String defaultClusterName) {
        this.jeapKafkaBeanNames = new JeapKafkaBeanNames(defaultClusterName);
    }

    String getTransactionalOutboxBeanName(String clusterName) {
        return jeapKafkaBeanNames.getBeanName(clusterName, "TransactionalOutbox");
    }

    String getDeferredMessageSenderBeanName(String clusterName) {
        return jeapKafkaBeanNames.getBeanName(clusterName, "DeferredMessageSender");
    }

    String getOutboxAvroMessageSerializerBeanName(String clusterName) {
        return jeapKafkaBeanNames.getBeanName(clusterName, "OutboxAvroMessageSerializer");
    }
}
