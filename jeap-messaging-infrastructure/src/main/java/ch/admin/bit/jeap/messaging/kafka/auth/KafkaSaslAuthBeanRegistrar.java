package ch.admin.bit.jeap.messaging.kafka.auth;

import ch.admin.bit.jeap.messaging.kafka.properties.cluster.ClusterProperties;
import org.apache.kafka.common.security.auth.SecurityProtocol;

class KafkaSaslAuthBeanRegistrar extends AbstractAuthBeanRegistrar {

    KafkaSaslAuthBeanRegistrar() {
        super(KafkaSaslAuthProperties.class);
    }

    @Override
    protected boolean shouldRegisterKafkaAuthPropertiesBeanForCluster(ClusterProperties clusterProperties) {
        return !clusterProperties.awsMskIamAuthActive() && !SecurityProtocol.SSL.equals(clusterProperties.getSecurityProtocol());
    }
}
