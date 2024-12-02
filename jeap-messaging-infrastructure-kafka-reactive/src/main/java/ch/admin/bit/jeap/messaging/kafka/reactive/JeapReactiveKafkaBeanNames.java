package ch.admin.bit.jeap.messaging.kafka.reactive;

import java.util.Objects;

public class JeapReactiveKafkaBeanNames {

    private final String defaultClusterName;

    public JeapReactiveKafkaBeanNames(String defaultClusterName) {
        this.defaultClusterName = Objects.requireNonNull(defaultClusterName, "defaultClusterName");
    }

    public boolean isPrimaryBean(String clusterName) {
        return defaultClusterName.equals(clusterName);
    }

    public String getSenderOptionsBeanName(String clusterName) {
        return getBeanName(clusterName, "SenderOptions");
    }

    public String getReceiverOptionsBeanName(String clusterName) {
        return getBeanName(clusterName, "ReceiverOptions");
    }

    public String getReactiveKafkaProducerTemplateBeanName(String clusterName) {
        return getBeanName(clusterName, "ProducerTemplate");
    }

    public String getSenderReceiverOptionsFactoryBeanName(String clusterName) {
        return getBeanName(clusterName, "SenderReceiverOptionsFactory");
    }

    /**
     * @return Bean name for a reactive Kafka bean. Prefix with cluster name for non-primary beans. For example:
     * reactiveKafkaProducerTemplate if primary, clusternameReactiveKafkaProducerTemplate if non-primary.
     */
    public String getBeanName(String clusterName, String beanType) {
        boolean isPrimaryBean = clusterName == null || isPrimaryBean(clusterName);
        String prefix = isPrimaryBean ? "reactiveKafka" : (clusterName + "ReactiveKafka");
        return prefix + beanType;
    }

}
