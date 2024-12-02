package ch.admin.bit.jeap.messaging.kafka.auth.aws.iam;

import ch.admin.bit.jeap.messaging.kafka.auth.AbstractAuthBeanRegistrar;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.ClusterProperties;

class AwsMskAuthBeanRegistrar extends AbstractAuthBeanRegistrar {

    AwsMskAuthBeanRegistrar() {
        super(AwsMskKafkaAuthProperties.class);
    }

    @Override
    protected boolean shouldRegisterKafkaAuthPropertiesBeanForCluster(ClusterProperties clusterProperties) {
        return clusterProperties.awsMskIamAuthActive();
    }
}
