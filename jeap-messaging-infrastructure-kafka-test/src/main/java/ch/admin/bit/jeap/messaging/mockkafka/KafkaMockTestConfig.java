package ch.admin.bit.jeap.messaging.mockkafka;

import ch.admin.bit.jeap.messaging.kafka.KafkaConfiguration;
import ch.admin.bit.jeap.messaging.kafka.KafkaConsumerConfiguration;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.config.ConfluentSchemaRegistryAutoConfiguration;
import lombok.Getter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;

@TestConfiguration
@EnableAutoConfiguration(exclude = {
        ConfluentSchemaRegistryAutoConfiguration.class,
        KafkaAutoConfiguration.class,
        KafkaConfiguration.class,
        KafkaConsumerConfiguration.class
})
@Getter
public class KafkaMockTestConfig {

    @MockBean
    private KafkaTemplate<?, ?> kafkaTemplate;

    @MockBean
    private DefaultErrorHandler errorHandler;

    @MockBean
    private KafkaConfiguration kafkaConfiguration;

    @MockBean
    private KafkaProperties kafkaProperties;

}
