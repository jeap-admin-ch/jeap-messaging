package ch.admin.bit.jeap.messaging.kafka.auth;

import ch.admin.bit.jeap.messaging.kafka.properties.cluster.ClusterProperties;
import org.apache.kafka.common.security.auth.SecurityProtocol;

class KafkaSslAuthBeanRegistrar extends AbstractAuthBeanRegistrar {

    KafkaSslAuthBeanRegistrar() {
        super(KafkaSslAuthProperties.class);
    }

    @Override
    protected boolean shouldRegisterKafkaAuthPropertiesBeanForCluster(ClusterProperties clusterProperties) {
        return SecurityProtocol.SSL.equals(clusterProperties.getSecurityProtocol());
    }
}
