package ch.admin.bit.jeap.messaging.kafka.properties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.kafka.listener.ListenerContainerRegistry;

@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerPropertiesValidator implements ApplicationListener<ContextRefreshedEvent> {

    private final ListenerContainerRegistry registry;
    private final KafkaProperties kafkaProperties;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        int listenerContainerCount = registry.getListenerContainerIds().size();
        if (listenerContainerCount > 0) {
            log.info("{} kafka listener containers registered - validating consumer properties", listenerContainerCount);
            validatePropertyHasValue("systemName", kafkaProperties.getSystemName());
            validatePropertyHasValue("serviceName", kafkaProperties.getServiceName());
            kafkaProperties.clusterNames().forEach(clusterName ->
                    validatePropertyHasValue("errorTopicName", kafkaProperties.getErrorTopicName(clusterName)));
        } else {
            log.debug("No listener containers registered - skipping consumer property validation");
        }
    }

    private void validatePropertyHasValue(String propertyName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Property jeap.messaging.kafka." + propertyName +
                    " is missing a valid value. This property needs to be set for applications consuming kafka records.");
        }
    }
}
