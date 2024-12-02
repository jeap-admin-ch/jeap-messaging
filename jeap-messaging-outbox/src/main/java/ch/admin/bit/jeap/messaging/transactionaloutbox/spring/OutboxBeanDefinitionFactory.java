package ch.admin.bit.jeap.messaging.transactionaloutbox.spring;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.GenericBeanDefinition;

@RequiredArgsConstructor
class OutboxBeanDefinitionFactory {
    private final KafkaProperties kafkaProperties;
    private final JeapKafkaBeanNames jeapKafkaBeanNames;

    GenericBeanDefinition createSerializerBeanDefinition(String clusterName) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName("ch.admin.bit.jeap.messaging.transactionaloutbox.messaging.KafkaAvroMessageSerializer");
        ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
        String serdeBeanName = jeapKafkaBeanNames.getKafkaAvroSerdeProviderBeanName(clusterName);
        constructorArgs.addGenericArgumentValue(new RuntimeBeanReference(serdeBeanName));
        beanDefinition.setConstructorArgumentValues(constructorArgs);

        // Add qualifier and mark as (non-)primary
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(isPrimaryProducerCluster(clusterName));
        return beanDefinition;
    }

    GenericBeanDefinition createDeferredMessageSenderBeanDefinition(String clusterName) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName("ch.admin.bit.jeap.messaging.transactionaloutbox.messaging.KafkaDeferredMessageSender");
        beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
        String producerFactoryBeanName = jeapKafkaBeanNames.getProducerFactoryBeanName(clusterName);
        constructorArgs.addGenericArgumentValue(new RuntimeBeanReference(producerFactoryBeanName));
        beanDefinition.setConstructorArgumentValues(constructorArgs);

        // Add qualifier and mark as (non-)primary
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(isPrimaryProducerCluster(clusterName));
        return beanDefinition;
    }

    GenericBeanDefinition createTransactionalOutboxBeanDefinition(String clusterName, String serializerBeanName) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(TransactionalOutbox.class);
        ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
        constructorArgs.addGenericArgumentValue(clusterName);
        constructorArgs.addGenericArgumentValue(new RuntimeBeanReference(serializerBeanName));
        beanDefinition.setConstructorArgumentValues(constructorArgs);

        // Add qualifier and mark as (non-)primary
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clusterName));
        beanDefinition.setPrimary(isPrimaryProducerCluster(clusterName));
        return beanDefinition;
    }

    private boolean isPrimaryProducerCluster(String clusterName) {
        return jeapKafkaBeanNames.isPrimaryProducerCluster(kafkaProperties.getDefaultProducerClusterOverride(), clusterName);
    }
}
