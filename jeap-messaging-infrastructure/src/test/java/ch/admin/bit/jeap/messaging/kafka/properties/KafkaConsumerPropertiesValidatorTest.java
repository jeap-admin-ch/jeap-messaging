package ch.admin.bit.jeap.messaging.kafka.properties;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.ListenerContainerRegistry;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class KafkaConsumerPropertiesValidatorTest {

    @Test
    void onApplicationEvent_consumersPresentAndAllPropertiesSet() {
        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.setServiceName("my-service");
        kafkaProperties.setSystemName("my-system");
        kafkaProperties.setErrorTopicName("error-topic");

        ListenerContainerRegistry registry = mock(ListenerContainerRegistry.class);
        doReturn(Set.of("1")).when(registry).getListenerContainerIds();

        KafkaConsumerPropertiesValidator validator = new KafkaConsumerPropertiesValidator(registry, kafkaProperties);

        assertDoesNotThrow(() ->
                validator.onApplicationEvent(null));
    }

    @Test
    void onApplicationEvent_consumersPresentAndPropertyMissing() {
        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.setServiceName("my-service");
        kafkaProperties.setSystemName("my-system");
        kafkaProperties.afterPropertiesSet();

        ListenerContainerRegistry registry = mock(ListenerContainerRegistry.class);
        doReturn(Set.of("1")).when(registry).getListenerContainerIds();

        KafkaConsumerPropertiesValidator validator = new KafkaConsumerPropertiesValidator(registry, kafkaProperties);

        assertThrows(IllegalStateException.class, () ->
                validator.onApplicationEvent(null));
    }

    @Test
    void onApplicationEvent_noConsumersPresentAndNoPropertiesSet() {
        KafkaProperties kafkaProperties = new KafkaProperties();
        ListenerContainerRegistry registry = mock(ListenerContainerRegistry.class);
        doReturn(Set.of()).when(registry).getListenerContainerIds();

        KafkaConsumerPropertiesValidator validator = new KafkaConsumerPropertiesValidator(registry, kafkaProperties);

        assertDoesNotThrow(() ->
                validator.onApplicationEvent(null));
    }
}