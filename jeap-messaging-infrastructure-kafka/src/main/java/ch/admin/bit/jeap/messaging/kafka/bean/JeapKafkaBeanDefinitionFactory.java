package ch.admin.bit.jeap.messaging.kafka.bean;

import ch.admin.bit.jeap.messaging.kafka.KafkaConfiguration;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.messaging.kafka.tracing.JeapKafkaTracing;
import ch.admin.bit.jeap.messaging.kafka.tracing.TracerBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.transaction.KafkaTransactionManager;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
class JeapKafkaBeanDefinitionFactory {
    private final BeanFactory beanFactory;
    private final KafkaProperties springKafkaProperties;
    private final JeapKafkaBeanNames jeapKafkaBeanNames;
    private final String defaultProducerClusterOverride;

    @SuppressWarnings({"unchecked", "rawtypes"})
    GenericBeanDefinition createListenerContainerFactory(String clusterName) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(ConcurrentKafkaListenerContainerFactory.class);
        beanDefinition.setInstanceSupplier(() -> {
            ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();

            String consumerFactoryBeanName = jeapKafkaBeanNames.getConsumerFactoryBeanName(clusterName);
            ConsumerFactory<Object, Object> kafkaConsumerFactory = (ConsumerFactory<Object, Object>) beanFactory.getBean(consumerFactoryBeanName);
            String kafkaTemplateBeanName = jeapKafkaBeanNames.getKafkaTemplateBeanName(clusterName);
            KafkaTemplate<Object, Object> kafkaTemplate = (KafkaTemplate<Object, Object>) beanFactory.getBean(kafkaTemplateBeanName);
            String kafkaAdminBeanName = jeapKafkaBeanNames.getAdminBeanName(clusterName);
            KafkaAdmin kafkaAdmin = (KafkaAdmin) beanFactory.getBean(kafkaAdminBeanName);

            // kafkaListenerContainerFactoryConfigurer is provided by KafkaAnnotationDrivenConfiguration
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer = (ConcurrentKafkaListenerContainerFactoryConfigurer) beanFactory.getBean("kafkaListenerContainerFactoryConfigurer");
            configurer.configure(factory, kafkaConsumerFactory);
            factory.setReplyTemplate(kafkaTemplate);
            ObjectProvider<KafkaTransactionManager> kafkaTransactionManager = beanFactory.getBeanProvider(KafkaTransactionManager.class);
            kafkaTransactionManager.ifAvailable(factory.getContainerProperties()::setTransactionManager);
            ObjectProvider<ContainerCustomizer> containerCustomizer = beanFactory.getBeanProvider(ContainerCustomizer.class);
            factory.setContainerCustomizer(container -> {
                containerCustomizer.ifAvailable(c -> c.configure(container));
                // Make sure new containers have the kafkaAdmin instance for their cluster injected
                container.setKafkaAdmin(kafkaAdmin);
            });

            ObjectProvider<TracerBridge> tracerBridge = beanFactory.getBeanProvider(TracerBridge.class);
            tracerBridge.ifAvailable(bean -> {
                log.debug("Kafka tracing is active: enable observation on kafkaTemplate and kafkaListenerContainerFactory...");
                factory.getContainerProperties().setObservationEnabled(true);
                kafkaTemplate.setObservationEnabled(true);
            });

            return factory;
        });

        // Add qualifier and mark as (non-)primary
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(jeapKafkaBeanNames.isPrimaryBean(clusterName));
        return beanDefinition;
    }

    GenericBeanDefinition createKafkaAdmin(String clusterName) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(KafkaAdmin.class);
        beanDefinition.setInstanceSupplier(() -> {
            Map<String, Object> configs = new HashMap<>(springKafkaProperties.buildAdminProperties(null));
            KafkaConfiguration jeapKafkaConfiguration = beanFactory.getBean(KafkaConfiguration.class);
            configs.putAll(jeapKafkaConfiguration.adminConfig(clusterName));

            KafkaAdmin kafkaAdmin = new KafkaAdmin(configs);
            kafkaAdmin.setFatalIfBrokerNotAvailable(springKafkaProperties.getAdmin().isFailFast());
            return kafkaAdmin;
        });

        // Add qualifier and mark as (non-)primary
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(jeapKafkaBeanNames.isPrimaryBean(clusterName));
        return beanDefinition;
    }

    GenericBeanDefinition createKafkaProducerFactory(String clusterName) {
        Map<String, Object> properties = springKafkaProperties.buildProducerProperties(null);

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(DefaultKafkaProducerFactory.class);
        beanDefinition.setInstanceSupplier(() -> {
            DefaultKafkaProducerFactory<Object, Object> factory = new DefaultKafkaProducerFactory<>(properties);
            String transactionIdPrefix = springKafkaProperties.getProducer().getTransactionIdPrefix();
            if (transactionIdPrefix != null) {
                factory.setTransactionIdPrefix(transactionIdPrefix);
            }

            KafkaAvroSerdeProvider kafkaAvroSerdeProvider = (KafkaAvroSerdeProvider) beanFactory.getBean(
                    jeapKafkaBeanNames.getKafkaAvroSerdeProviderBeanName(clusterName));
            factory.setValueSerializer(kafkaAvroSerdeProvider.getValueSerializer());
            factory.setKeySerializer(kafkaAvroSerdeProvider.getKeySerializer());

            KafkaConfiguration jeapKafkaConfiguration = beanFactory.getBean(KafkaConfiguration.class);
            factory.updateConfigs(jeapKafkaConfiguration.producerConfig(clusterName));

            return factory;
        });

        // Add qualifier and mark as (non-)primary
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(isPrimaryProducerCluster(clusterName));
        return beanDefinition;
    }

    GenericBeanDefinition createTransactionManager(String clusterName) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(KafkaTransactionManager.class);
        ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
        String producerFactoryBeanName = jeapKafkaBeanNames.getProducerFactoryBeanName(clusterName);
        constructorArgs.addGenericArgumentValue(new RuntimeBeanReference(producerFactoryBeanName));
        beanDefinition.setConstructorArgumentValues(constructorArgs);

        // Add qualifier and mark as (non-)primary
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(jeapKafkaBeanNames.isPrimaryBean(clusterName));
        return beanDefinition;
    }

    GenericBeanDefinition createKafkaConsumerFactory(String clusterName) {
        Map<String, Object> properties = springKafkaProperties.buildConsumerProperties(null);

        // Create KafkaConsumerFactory and pass constructor argument
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(DefaultKafkaConsumerFactory.class);
        beanDefinition.setInstanceSupplier(() -> {
            DefaultKafkaConsumerFactory<Object, Object> factory = new DefaultKafkaConsumerFactory<>(properties);
            // Only available if jeap-monitoring has been activated
            ObjectProvider<JeapKafkaTracing> jeapKafkaTracingProvider = beanFactory.getBeanProvider(JeapKafkaTracing.class);
            jeapKafkaTracingProvider.ifAvailable(jeapKafkaTracing -> factory.addPostProcessor(jeapKafkaTracing::consumer));

            KafkaConfiguration jeapKafkaConfiguration = beanFactory.getBean(KafkaConfiguration.class);
            factory.updateConfigs(jeapKafkaConfiguration.consumerConfig(clusterName));

            return factory;
        });

        // Add qualifier and mark as (non-)primary
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(jeapKafkaBeanNames.isPrimaryBean(clusterName));
        return beanDefinition;
    }

    GenericBeanDefinition createKafkaTemplate(String clusterName) {
        // Create KafkaTemplate and pass constructor argument
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(KafkaTemplate.class);
        beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
        String producerFactoryBeanName = jeapKafkaBeanNames.getProducerFactoryBeanName(clusterName);
        constructorArgs.addGenericArgumentValue(new RuntimeBeanReference(producerFactoryBeanName));
        beanDefinition.setConstructorArgumentValues(constructorArgs);

        // Set properties on KafkaTemplate bean (see spring KafkaAutoConfiguration)
        MutablePropertyValues propertyValues = new MutablePropertyValues();
        propertyValues.addPropertyValue("defaultTopic", springKafkaProperties.getTemplate().getDefaultTopic());
        propertyValues.addPropertyValue("transactionIdPrefix", springKafkaProperties.getTemplate().getTransactionIdPrefix());
        propertyValues.addPropertyValue("producerListener", new RuntimeBeanReference(ProducerListener.class));
        String adminBeanName = jeapKafkaBeanNames.getAdminBeanName(clusterName);
        propertyValues.addPropertyValue("kafkaAdmin", new RuntimeBeanReference(adminBeanName));
        setRecordMessageConverterIfSingleCandidateBeanReference(propertyValues);
        beanDefinition.setPropertyValues(propertyValues);

        // Add qualifier and mark as (non-)primary
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(isPrimaryProducerCluster(clusterName));
        return beanDefinition;
    }

    private void setRecordMessageConverterIfSingleCandidateBeanReference(MutablePropertyValues propertyValues) {
        if (beanFactory instanceof ListableBeanFactory listableBeanFactory) {
            String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(listableBeanFactory, RecordMessageConverter.class, false, false);
            if (beanNamesForType.length == 1) {
                propertyValues.addPropertyValue("recordMessageConverter", new RuntimeBeanReference(beanNamesForType[0]));
            }
        }
    }

    private boolean isPrimaryProducerCluster(String clusterName) {
        return jeapKafkaBeanNames.isPrimaryProducerCluster(defaultProducerClusterOverride, clusterName);
    }
}
