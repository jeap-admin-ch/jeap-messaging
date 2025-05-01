package ch.admin.bit.jeap.messaging.sequentialinbox.kafka;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.MessageTypeMetadata;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.messaging.sequentialinbox.inbox.SequentialInboxService;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxException;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxMessageHandler;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class KafkaSequentialInboxMessageConsumerFactory {

    private final KafkaProperties kafkaProperties;
    private final BeanFactory beanFactory;
    private final JeapKafkaBeanNames jeapKafkaBeanNames;
    private final SequentialInboxService sequentialInboxService;
    private final ContractsValidator contractsValidator;

    private final List<ConcurrentMessageListenerContainer<AvroMessageKey, AvroMessage>> containers = new CopyOnWriteArrayList<>();

    public KafkaSequentialInboxMessageConsumerFactory(KafkaProperties kafkaProperties, BeanFactory beanFactory, SequentialInboxService sequentialInboxService, ContractsValidator contractsValidator) {
        this.kafkaProperties = kafkaProperties;
        this.beanFactory = beanFactory;
        this.jeapKafkaBeanNames = new JeapKafkaBeanNames(kafkaProperties.getDefaultClusterName());
        this.sequentialInboxService = sequentialInboxService;
        this.contractsValidator = contractsValidator;
    }

    public void startConsumer(String topicName, String messageType, String clusterName, SequentialInboxMessageHandler messageHandler) {
        if (!StringUtils.hasText(clusterName)) {
            clusterName = kafkaProperties.getDefaultClusterName();
        }
        if (!StringUtils.hasText(topicName)) {
            topicName = getDefaultTopicForMessageType(messageHandler.getMessageTypeClass());
        }
        contractsValidator.ensureConsumerContract(messageType, topicName);

        log.info("Starting sequential inbox message listener for messageType '{}' on topic '{}' on cluster '{}'", messageType, topicName, clusterName);
        KafkaSequentialInboxMessageListener listener = new KafkaSequentialInboxMessageListener(messageHandler, sequentialInboxService);
        startConsumer(topicName, clusterName, listener);
    }

    private String getDefaultTopicForMessageType(Class<AvroMessage> messageTypeClass) {
        try {
            Class<?> messageTypeMetadataClass = Arrays.stream(messageTypeClass.getDeclaredClasses())
                    .filter(MessageTypeMetadata.class::isAssignableFrom)
                    .findFirst().orElseThrow(() -> SequentialInboxException.typeRefNotFound(messageTypeClass));
            String defaultTopic = (String) messageTypeMetadataClass.getDeclaredField("DEFAULT_TOPIC").get(messageTypeClass);
            if (defaultTopic == null) {
                throw SequentialInboxException.defaultTopicNotFound(messageTypeClass);
            }
            return defaultTopic;
        } catch (Exception e) {
            log.error("Could not get default topic for message type '{}'", messageTypeClass.getName(), e);
            throw SequentialInboxException.gettingDefaultTopicFailed(messageTypeClass, e);
        }
    }

    private void startConsumer(String topicName, String clusterName, AcknowledgingMessageListener<AvroMessageKey, AvroMessage> messageListener) {
        ConcurrentMessageListenerContainer<AvroMessageKey, AvroMessage> container =
                getKafkaListenerContainerFactory(clusterName).createContainer(topicName);
        // The inbox invokes the JeapKafkaMessageCallback explicitly, avoid duplicate invocations by the interceptor
        // The inbox does not support record interceptors in general as buffered records might be consumed/buffered by
        // the inbox and not by the application's business logic. The inbox will then invoke the message handler as
        // soon as the release condition for the message is satisfied.
        container.setRecordInterceptor(null);
        container.setupMessageListener(messageListener);
        container.start();
        containers.add(container);
    }

    @SuppressWarnings("unchecked")
    private ConcurrentKafkaListenerContainerFactory<AvroMessageKey, AvroMessage> getKafkaListenerContainerFactory(String clusterName) {
        try {
            return (ConcurrentKafkaListenerContainerFactory<AvroMessageKey, AvroMessage>) beanFactory.getBean(jeapKafkaBeanNames.getListenerContainerFactoryBeanName(clusterName));
        } catch (NoSuchBeanDefinitionException exception) {
            log.error("No kafkaListenerContainerFactory found for cluster with name '{}'", clusterName);
            throw new IllegalStateException("No kafkaListenerContainerFactory found for cluster with name " + clusterName);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping all message listener containers...");
        containers.forEach(concurrentMessageListenerContainer ->
                concurrentMessageListenerContainer.stop(true));
    }

    public List<ConcurrentMessageListenerContainer<AvroMessageKey, AvroMessage>> getContainers() {
        return List.copyOf(containers);
    }
}
