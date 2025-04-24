package ch.admin.bit.jeap.messaging.kafka.bean;

import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaPropertyFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;

/**
 * A {@link ImportBeanDefinitionRegistrar} that registers a set of Spring Kafka beans for each configured kafka
 * cluster. This class mimics the bean definitions in {@link org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration},
 * with the difference that it supports creating multiple sets of beans and not just for a single kafka cluster.
 */
@Slf4j
public class JeapKafkaBeanRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    @Setter
    private Environment environment;
    @Setter
    private BeanFactory beanFactory;
    private JeapKafkaBeanDefinitionFactory beanDefinitionFactory;
    private JeapKafkaBeanNames beanNames;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        JeapKafkaPropertyFactory propertyFactory = JeapKafkaPropertyFactory.create(environment, beanFactory);
        beanNames = new JeapKafkaBeanNames(propertyFactory.getJeapKafkaProperties().getDefaultClusterName());
        beanDefinitionFactory = new JeapKafkaBeanDefinitionFactory(
                beanFactory, propertyFactory.getSpringKafkaProperties(), beanNames, propertyFactory.getJeapKafkaProperties().getDefaultProducerClusterOverride());

        propertyFactory.getClusterNames().forEach(clusterName ->
                registerBeansForClusterName(registry, clusterName));
    }

    private void registerBeansForClusterName(BeanDefinitionRegistry registry, String clusterName) {
        registerKafkaTemplate(registry, clusterName);
        registerKafkaProducerFactory(registry, clusterName);
        registerKafkaTransactionManager(registry, clusterName);
        registerKafkaConsumerFactory(registry, clusterName);
        registerKafkaAdmin(registry, clusterName);
        registerKafkaListenerContainerFactory(registry, clusterName);
    }

    /**
     * This registers a KafkaTemplate per Kafka Cluster. The bean definition is a programmatic clone of the bean
     * definition in {@link org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration#kafkaTemplate(ProducerFactory, ProducerListener, ObjectProvider)}.
     * The registered bean receives a {@code @Qualifier(clusterName)} annotation and may be marked as primary bean.
     */
    private void registerKafkaTemplate(BeanDefinitionRegistry registry, String clusterName) {
        String beanName = beanNames.getKafkaTemplateBeanName(clusterName);
        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = beanDefinitionFactory.createKafkaTemplate(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private void registerKafkaListenerContainerFactory(BeanDefinitionRegistry registry, String clusterName) {
        String beanName = beanNames.getListenerContainerFactoryBeanName(clusterName);
        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = beanDefinitionFactory.createListenerContainerFactory(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private void registerKafkaAdmin(BeanDefinitionRegistry registry, String clusterName) {
        String beanName = beanNames.getAdminBeanName(clusterName);
        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = beanDefinitionFactory.createKafkaAdmin(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private void registerKafkaProducerFactory(BeanDefinitionRegistry registry, String clusterName) {
        String beanName = beanNames.getProducerFactoryBeanName(clusterName);
        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = beanDefinitionFactory.createKafkaProducerFactory(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private void registerKafkaTransactionManager(BeanDefinitionRegistry registry, String clusterName) {
        // Mimic @ConditionalOnProperty(name = "spring.kafka.producer.transaction-id-prefix") from KafkaAutoConfiguration
        if (!environment.containsProperty("spring.kafka.producer.transaction-id-prefix")) {
            return;
        }

        String beanName = beanNames.getTransactionManagerBeanName(clusterName);
        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = beanDefinitionFactory.createTransactionManager(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private void registerKafkaConsumerFactory(BeanDefinitionRegistry registry, String clusterName) {
        String beanName = beanNames.getConsumerFactoryBeanName(clusterName);
        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = beanDefinitionFactory.createKafkaConsumerFactory(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }
}
