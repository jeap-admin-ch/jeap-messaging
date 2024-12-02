package ch.admin.bit.jeap.messaging.kafka.reactive;

import ch.admin.bit.jeap.messaging.kafka.KafkaConfiguration;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

import java.util.Optional;

@RequiredArgsConstructor
class JeapReactiveKafkaBeanDefinitionFactory {

    private final BeanFactory beanFactory;
    private final JeapKafkaBeanNames jeapKafkaBeanNames;
    private final JeapReactiveKafkaBeanNames jeapReactiveKafkaBeanNames;
    private final KafkaProperties springKafkaProperties;
    private final String defaultProducerClusterOverride;

    GenericBeanDefinition createJeapReactiveKafkaSenderReceiverOptionsFactory(String clusterName) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(JeapSenderReceiverOptionsFactory.class);
        beanDefinition.setInstanceSupplier(() -> {
            KafkaConfiguration jeapKafkaConfiguration = beanFactory.getBean(KafkaConfiguration.class);
            String kafkaAvroSerdeProviderBeanName = jeapKafkaBeanNames.getKafkaAvroSerdeProviderBeanName(clusterName);
            KafkaAvroSerdeProvider kafkaAvroSerdeProvider = beanFactory.getBean(kafkaAvroSerdeProviderBeanName, KafkaAvroSerdeProvider.class);
            return new JeapSenderReceiverOptionsFactory(clusterName, springKafkaProperties, jeapKafkaConfiguration, kafkaAvroSerdeProvider);
        });
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(jeapKafkaBeanNames.isPrimaryBean(clusterName));
        return beanDefinition;
    }

    GenericBeanDefinition createSenderOptions(String clusterName) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(SenderOptions.class);
        beanDefinition.setInstanceSupplier(() -> getSenderReceiverOptionsFactory(clusterName).createSenderOptions());
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(isPrimaryProducerCluster(clusterName));
        return beanDefinition;
    }

    GenericBeanDefinition createReceiverOptions(String clusterName) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(ReceiverOptions.class);
        beanDefinition.setInstanceSupplier(() -> getSenderReceiverOptionsFactory(clusterName).createReceiverOptions());
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(jeapKafkaBeanNames.isPrimaryBean(clusterName));
        return beanDefinition;
    }

    GenericBeanDefinition createReactiveKafkaProducerTemplate(String clusterName) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(ReactiveKafkaProducerTemplate.class);
        beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
        String senderOptionsBeanName = jeapReactiveKafkaBeanNames.getSenderOptionsBeanName(clusterName);
        constructorArgs.addGenericArgumentValue(new RuntimeBeanReference(senderOptionsBeanName));
        getRecordMessageConverterIfSingleCandidate().ifPresent(constructorArgs::addGenericArgumentValue);
        beanDefinition.setConstructorArgumentValues(constructorArgs);
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(isPrimaryProducerCluster(clusterName));
        return beanDefinition;
    }

    private boolean isPrimaryProducerCluster(String clusterName) {
        return jeapKafkaBeanNames.isPrimaryProducerCluster(defaultProducerClusterOverride, clusterName);
    }

    private Optional<RuntimeBeanReference> getRecordMessageConverterIfSingleCandidate() {
        if (beanFactory instanceof ListableBeanFactory listableBeanFactory) {
            String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(listableBeanFactory, RecordMessageConverter.class, false, false);
            if (beanNamesForType.length == 1) {
                return Optional.of(new RuntimeBeanReference(beanNamesForType[0]));
            }
        }
        return Optional.empty();
    }

    private JeapSenderReceiverOptionsFactory getSenderReceiverOptionsFactory(String clusterName) {
        String senderReceiverOptionsFactoryBeanName = jeapReactiveKafkaBeanNames.getSenderReceiverOptionsFactoryBeanName(clusterName);
        return beanFactory.getBean(senderReceiverOptionsFactoryBeanName, JeapSenderReceiverOptionsFactory.class);
    }

}
