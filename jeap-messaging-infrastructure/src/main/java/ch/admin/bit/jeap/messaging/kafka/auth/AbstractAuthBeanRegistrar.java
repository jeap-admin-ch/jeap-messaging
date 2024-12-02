package ch.admin.bit.jeap.messaging.kafka.auth;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.ClusterProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaPropertyFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

@Slf4j
public abstract class AbstractAuthBeanRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {
    private final Class<? extends KafkaAuthProperties> authPropertiesType;
    @Setter
    protected Environment environment;
    @Setter
    protected BeanFactory beanFactory;
    protected KafkaProperties kafkaProperties;
    private JeapKafkaBeanNames jeapKafkaBeanNames;

    public AbstractAuthBeanRegistrar(Class<? extends KafkaAuthProperties> authPropertiesType) {
        this.authPropertiesType = authPropertiesType;
    }

    protected abstract boolean shouldRegisterKafkaAuthPropertiesBeanForCluster(ClusterProperties clusterProperties);

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        kafkaProperties = JeapKafkaPropertyFactory.createJeapKafkaProperties(environment);
        jeapKafkaBeanNames = new JeapKafkaBeanNames(kafkaProperties.getDefaultClusterName());
        kafkaProperties.clusterNames().forEach(clusterName -> {
            ClusterProperties clusterProperties = kafkaProperties.clusterProperties(clusterName).orElseThrow();
            if (shouldRegisterKafkaAuthPropertiesBeanForCluster(clusterProperties)) {
                registerKafkaAuthPropertiesBeanForCluster(registry, clusterName);
            }
        });
    }

    private void registerKafkaAuthPropertiesBeanForCluster(BeanDefinitionRegistry registry, String clusterName) {
        String beanName = jeapKafkaBeanNames.getAuthPropertiesBeanName(clusterName);

        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = createKafkaAuthPropertiesBeanForCluster(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private GenericBeanDefinition createKafkaAuthPropertiesBeanForCluster(String clusterName) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(authPropertiesType);

        // Add qualifier and mark as (non-)primary
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(jeapKafkaBeanNames.isPrimaryBean(clusterName));
        return beanDefinition;
    }
}
