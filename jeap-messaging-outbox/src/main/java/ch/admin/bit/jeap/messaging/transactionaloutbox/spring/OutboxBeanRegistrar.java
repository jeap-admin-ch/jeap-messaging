package ch.admin.bit.jeap.messaging.transactionaloutbox.spring;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaPropertyFactory;
import lombok.Setter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Registers transactional outbox spring beans (serializer, transactional outbox, deferred message sender) for each
 * configured kafka cluster.
 */
public class OutboxBeanRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    @Setter
    protected Environment environment;
    @Setter
    protected BeanFactory beanFactory;
    private OutboxBeanDefinitionFactory beanDefinitionFactory;
    private OutboxBeanNames outboxBeanNames;


    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        KafkaProperties kafkaProperties = JeapKafkaPropertyFactory.createJeapKafkaProperties(environment);
        outboxBeanNames = new OutboxBeanNames(kafkaProperties.getDefaultClusterName());
        beanDefinitionFactory = new OutboxBeanDefinitionFactory(kafkaProperties, new JeapKafkaBeanNames(kafkaProperties.getDefaultClusterName()));

        kafkaProperties.clusterNames().forEach(clusterName ->
                registerOutboxBeanDefinitionsForCluster(registry, clusterName));
    }

    private void registerOutboxBeanDefinitionsForCluster(BeanDefinitionRegistry registry, String clusterName) {
        String serializerBeanName = registerSerializerBeanDefinition(registry, clusterName);
        registerDeferredMessageSenderBeanDefinition(registry, clusterName);
        registerTransactionalOutboxBeanDefinition(registry, clusterName, serializerBeanName);
    }

    private void registerTransactionalOutboxBeanDefinition(BeanDefinitionRegistry registry, String clusterName, String serializerBeanName) {
        String beanName = outboxBeanNames.getTransactionalOutboxBeanName(clusterName);

        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = beanDefinitionFactory.createTransactionalOutboxBeanDefinition(clusterName, serializerBeanName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private void registerDeferredMessageSenderBeanDefinition(BeanDefinitionRegistry registry, String clusterName) {
        String beanName = outboxBeanNames.getDeferredMessageSenderBeanName(clusterName);

        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = beanDefinitionFactory.createDeferredMessageSenderBeanDefinition(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private String registerSerializerBeanDefinition(BeanDefinitionRegistry registry, String clusterName) {
        String beanName = outboxBeanNames.getOutboxAvroMessageSerializerBeanName(clusterName);

        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = beanDefinitionFactory.createSerializerBeanDefinition(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }

        return beanName;
    }
}
