package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = TestConfig.class, properties = {
        "spring.application.name=jme-messaging-subscriber-service",
        "spring.kafka.listener.ack-count=42",
        "spring.kafka.producer.buffer-memory=303808",
        "spring.kafka.producer.retries=303",
        "spring.kafka.consumer.isolation-level=READ_COMMITTED",
        "spring.kafka.admin.properties.foo=bar"})
@Slf4j
@DirtiesContext
class CustomConfigurationIT extends KafkaIntegrationTestBase {
    @MockitoBean
    private MessageListener<JmeDeclarationCreatedEvent> jmeEventProcessor;
    @Autowired
    private ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory;
    @Autowired
    private ProducerFactory<?, ?> producerFactory;
    @Autowired
    private ConsumerFactory<?, ?> consumerFactory;
    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Test
    void customConfigurationPropertiesAreSetInCreatedBeans() {
        assertSame(MANUAL, kafkaListenerContainerFactory.getContainerProperties().getAckMode(),
                "Ack mode read from standard spring property, as set in jeap-messaging-kafka-defaults.properties");
        assertEquals("all", producerFactory.getConfigurationProperties().get("acks"),
                "Acks read from standard spring property, as set in jeap-messaging-kafka-defaults.properties");
        assertEquals(10, consumerFactory.getConfigurationProperties().get("max.poll.records"),
                "max.poll.records read from standard spring property, as set in jeap-messaging-kafka-defaults.properties");

        assertEquals(42, kafkaListenerContainerFactory.getContainerProperties().getAckCount(),
                "Ack count read from standard spring property when building listener container factory");
        assertEquals(303808L, producerFactory.getConfigurationProperties().get("buffer.memory"),
                "buffer-memory read from standard spring property when building producer");
        assertEquals("read_committed", consumerFactory.getConfigurationProperties().get("isolation.level"),
                "isolation.level read from standard spring property when building consumer");
        assertEquals("bar", kafkaAdmin.getConfigurationProperties().get("foo"),
                "foo read from standard spring property when building admin client");

        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId").message("customConfig").build());
        verify(jmeEventProcessor, timeout(TEST_TIMEOUT)).receive(any());
    }
}
