package ch.admin.bit.jeap.messaging.kafka.spring;

import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.ClusterProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import lombok.Setter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

public abstract class AbstractSchemaRegistryBeanRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {
    @Setter
    protected Environment environment;
    @Setter
    protected BeanFactory beanFactory;
    protected KafkaProperties kafkaProperties;
    private JeapKafkaBeanNames beanNames;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        kafkaProperties = JeapKafkaPropertyFactory.createJeapKafkaProperties(environment);
        beanNames = new JeapKafkaBeanNames(kafkaProperties.getDefaultClusterName());
        kafkaProperties.clusterNames().forEach(clusterName -> {
            ClusterProperties clusterProperties = kafkaProperties.clusterProperties(clusterName).orElseThrow();
            if (shouldRegisterSchemaRegistryBeansForCluster(clusterProperties)) {
                registerKafkaAvroSerdeProviderBeanForCluster(registry, clusterName);
            }
        });
    }

    protected abstract boolean shouldRegisterSchemaRegistryBeansForCluster(ClusterProperties clusterProperties);

    private void registerKafkaAvroSerdeProviderBeanForCluster(BeanDefinitionRegistry registry, String clusterName) {
        String beanName = beanNames.getKafkaAvroSerdeProviderBeanName(clusterName);

        if (!registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = createKafkaAvroSerdeProviderBeanDefinition(clusterName);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private GenericBeanDefinition createKafkaAvroSerdeProviderBeanDefinition(String clusterName) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(KafkaAvroSerdeProvider.class);
        beanDefinition.setInstanceSupplier(() -> {
            ObjectProvider<JeapKafkaAvroSerdeCryptoConfig> cryptoConfig = beanFactory.getBeanProvider(JeapKafkaAvroSerdeCryptoConfig.class);
            return createKafkaAvroSerializerProvider(clusterName, cryptoConfig.getIfAvailable());
        });

        // Add qualifier and mark as (non-)primary
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(beanNames.isPrimaryBean(clusterName));
        return beanDefinition;
    }

    protected abstract KafkaAvroSerdeProvider createKafkaAvroSerializerProvider(String clusterName, JeapKafkaAvroSerdeCryptoConfig cryptoConfig);
}
