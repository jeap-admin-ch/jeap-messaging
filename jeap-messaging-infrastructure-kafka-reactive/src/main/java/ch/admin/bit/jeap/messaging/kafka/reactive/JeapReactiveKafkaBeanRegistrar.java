package ch.admin.bit.jeap.messaging.kafka.reactive;

import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaPropertyFactory;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;


@Slf4j
class JeapReactiveKafkaBeanRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    @Setter
    private Environment environment;
    @Setter
    private BeanFactory beanFactory;
    private JeapReactiveKafkaBeanDefinitionFactory jeapReactiveKafkaBeanDefinitionFactory;
    private JeapReactiveKafkaBeanNames jeapReactiveKafkaBeanNames;

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        JeapKafkaPropertyFactory propertyFactory = JeapKafkaPropertyFactory.create(environment, beanFactory);
        ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties jeapKafkaProperties = propertyFactory.getJeapKafkaProperties();
        String defaultClusterName = jeapKafkaProperties.getDefaultClusterName();
        String defaultProducerClusterOverride = jeapKafkaProperties.getDefaultProducerClusterOverride();
        KafkaProperties springKafkaProperties = propertyFactory.getSpringKafkaProperties();
        JeapKafkaBeanNames jeapKafkaBeanNames = new JeapKafkaBeanNames(defaultClusterName);
        jeapReactiveKafkaBeanNames = new JeapReactiveKafkaBeanNames(defaultClusterName);
        jeapReactiveKafkaBeanDefinitionFactory = new JeapReactiveKafkaBeanDefinitionFactory(
                beanFactory, jeapKafkaBeanNames, jeapReactiveKafkaBeanNames, springKafkaProperties, defaultProducerClusterOverride);
        propertyFactory.getClusterNames().forEach(clusterName ->
                registerBeansForClusterName(registry, clusterName));
    }

    private void registerBeansForClusterName(BeanDefinitionRegistry registry, String clusterName) {
        registerJeapReactiveKafkaSenderReceiverOptionsFactory(registry, clusterName);
        registerSenderOptions(registry, clusterName);
        registerReceiverOptions(registry, clusterName);
        registerReactiveKafkaPoducerTemplate(registry, clusterName);
    }

    private void registerJeapReactiveKafkaSenderReceiverOptionsFactory(BeanDefinitionRegistry registry, String clusterName) {
        String beanName = jeapReactiveKafkaBeanNames.getSenderReceiverOptionsFactoryBeanName(clusterName);
        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = jeapReactiveKafkaBeanDefinitionFactory.createJeapReactiveKafkaSenderReceiverOptionsFactory(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private void registerSenderOptions(BeanDefinitionRegistry registry, String clusterName) {
        String beanName = jeapReactiveKafkaBeanNames.getSenderOptionsBeanName(clusterName);
        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = jeapReactiveKafkaBeanDefinitionFactory.createSenderOptions(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private void registerReceiverOptions(BeanDefinitionRegistry registry, String clusterName) {
        String beanName = jeapReactiveKafkaBeanNames.getReceiverOptionsBeanName(clusterName);
        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = jeapReactiveKafkaBeanDefinitionFactory.createReceiverOptions(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private void registerReactiveKafkaPoducerTemplate(BeanDefinitionRegistry registry, String clusterName) {
        String beanName = jeapReactiveKafkaBeanNames.getReactiveKafkaProducerTemplateBeanName(clusterName);
        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = jeapReactiveKafkaBeanDefinitionFactory.createReactiveKafkaProducerTemplate(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

}
