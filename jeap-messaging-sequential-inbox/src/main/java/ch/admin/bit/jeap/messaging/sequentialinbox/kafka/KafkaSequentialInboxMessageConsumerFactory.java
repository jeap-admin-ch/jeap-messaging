package ch.admin.bit.jeap.messaging.sequentialinbox.kafka;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class KafkaSequentialInboxMessageConsumerFactory {

    private final KafkaProperties kafkaProperties;

    private final BeanFactory beanFactory;

    private final JeapKafkaBeanNames jeapKafkaBeanNames;

    private final List<ConcurrentMessageListenerContainer<?, ?>> containers = new CopyOnWriteArrayList<>();

    public KafkaSequentialInboxMessageConsumerFactory(KafkaProperties kafkaProperties, BeanFactory beanFactory) {
        this.kafkaProperties = kafkaProperties;
        this.beanFactory = beanFactory;
        this.jeapKafkaBeanNames = new JeapKafkaBeanNames(kafkaProperties.getDefaultClusterName());
    }

    public void startConsumer(String topicName, String messageType, String clusterName, SequentialInboxMessageHandler messageHandler) {
        if (!StringUtils.hasText(clusterName)) {
            clusterName = kafkaProperties.getDefaultClusterName();
        }
        if (!StringUtils.hasText(topicName)) {
            topicName = getDefaultTopicForMessageType(messageType);
        }

        log.info("Starting domain message listener for messageType '{}' on topic '{}' on cluster '{}'", messageType, topicName, clusterName);
        KafkaSequentialInboxMessageListener listener = new KafkaSequentialInboxMessageListener(messageType, messageHandler);
        startConsumer(topicName, clusterName, listener);
    }

    private String getDefaultTopicForMessageType(String messageType) {
        try {
            Class<?> messageDescriptor = Class.forName(messageType + ".TypeRef");
            return (String) messageDescriptor.getField("DEFAULT_TOPIC").get(messageDescriptor);
        } catch (Exception e) {
            log.error("Could not default topic for message type '{}'", messageType, e);
            throw new IllegalStateException("Could not default topic for message type " + messageType, e);
        }
    }

    private void startConsumer(String topicName, String clusterName, AcknowledgingMessageListener<AvroMessageKey, AvroMessage> messageListener) {
        ConcurrentMessageListenerContainer<AvroMessageKey, AvroMessage> container = getKafkaListenerContainerFactory(clusterName).createContainer(topicName);
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
        containers.forEach(concurrentMessageListenerContainer -> concurrentMessageListenerContainer.stop(true));
    }
}
