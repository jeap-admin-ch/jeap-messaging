package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.crypto.KafkaCryptoConfiguration;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.mockkafka.KafkaMockTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = KafkaMockIT.MockItTestComponent.class, properties = {
        "spring.application.name=KafkaMockIT"})
@Import(KafkaMockTestConfig.class)
@EnableAutoConfiguration(exclude = {KafkaCryptoConfiguration.class})
class KafkaMockIT {

    @Autowired
    private KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;

    @Autowired
    private Optional<KafkaListenerContainerFactory<?>> kafkaListenerContainerFactory;

    @Autowired
    private Optional<KafkaListenerEndpointRegistry> kafkaListenerEndpointRegistry;

    @Test
    void sendMessage_kafkaNotLoaded_factoryNotPresent() {
        AvroMessage message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").message("message").build();
        kafkaTemplate.send("myTopic", message);
        assertThat(message).isNotNull();
        assertThat(kafkaListenerContainerFactory).isEmpty();
        assertThat(kafkaListenerEndpointRegistry).isEmpty();
    }

    @Component
    static class MockItTestComponent {
    }
}