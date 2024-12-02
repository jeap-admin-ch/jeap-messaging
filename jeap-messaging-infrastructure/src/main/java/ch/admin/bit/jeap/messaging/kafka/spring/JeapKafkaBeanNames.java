package ch.admin.bit.jeap.messaging.kafka.spring;

import java.util.Objects;

public class JeapKafkaBeanNames {

    private final String defaultClusterName;

    public JeapKafkaBeanNames(String defaultClusterName) {
        this.defaultClusterName = Objects.requireNonNull(defaultClusterName, "defaultClusterName");
    }

    public boolean isPrimaryBean(String clusterName) {
        return defaultClusterName.equals(clusterName);
    }

    public String getListenerContainerFactoryBeanName(String clusterName) {
        return getBeanName(clusterName, "ListenerContainerFactory");
    }

    public String getAdminBeanName(String clusterName) {
        return getBeanName(clusterName, "Admin");
    }

    public String getConsumerFactoryBeanName(String clusterName) {
        return getBeanName(clusterName, "ConsumerFactory");
    }

    public String getTransactionManagerBeanName(String clusterName) {
        return getBeanName(clusterName, "TransactionManager");
    }

    public String getProducerFactoryBeanName(String clusterName) {
        return getBeanName(clusterName, "ProducerFactory");
    }

    public String getKafkaTemplateBeanName(String clusterName) {
        return getBeanName(clusterName, "Template");
    }

    public String getKafkaAvroSerdeProviderBeanName(String clusterName) {
        return getBeanName(clusterName, "AvroSerdeProvider");
    }

    public String getAuthPropertiesBeanName(String clusterName) {
        return getBeanName(clusterName, "AuthProperties");
    }

    /**
     * @return Bean name for a Spring kafka bean. Prefix with cluster name for non-primary beans. For example:
     * kafkaTemplate if primary, clusternameKafkaTemplate if non-primary.
     */
    public String getBeanName(String clusterName, String beanType) {
        boolean isPrimaryBean = clusterName == null || isPrimaryBean(clusterName);
        String prefix = isPrimaryBean ? "kafka" : (clusterName + "Kafka");
        return prefix + beanType;
    }

    public boolean isPrimaryProducerCluster(String defaultProducerClusterOverride, String clusterName) {
        if (defaultProducerClusterOverride != null) {
            return defaultProducerClusterOverride.equals(clusterName);
        }
        return isPrimaryBean(clusterName);
    }
}
