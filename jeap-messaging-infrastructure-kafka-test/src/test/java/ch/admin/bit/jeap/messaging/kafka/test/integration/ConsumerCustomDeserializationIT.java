package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeSimpleTestEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.BeanReferenceMessageKey;
import ch.admin.bit.jme.test.BeanReferenceMessageKeyV2;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestV2Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {TestConfig.class}, properties = {
        "spring.application.name=ConsumerCustomDeserializationIT",
        "jeap.messaging.kafka.exposeMessageKeyToConsumer=true"
})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
@DirtiesContext
class ConsumerCustomDeserializationIT extends KafkaIntegrationTestBase {

    @MockitoBean
    private MessageListener<JmeSimpleTestV2Event> jmeEventProcessor;


    @MockitoBean
    private MessageListener<JmeDeclarationCreatedEvent> jmeDeclarationCreatedEventProcessor;

    @Autowired
    private JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer consumer;

    @Test
    void testConsumeMessageWithCustomDeserializationForValue() {
        JmeSimpleTestEvent message = JmeSimpleTestEventBuilder.create()
                .idempotenceId("idempotenceId")
                .message("consumeOldEvent")
                .build();
        sendSync(JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer.TOPIC_NAME, message);

        ArgumentCaptor<JmeSimpleTestV2Event> argumentCaptor = ArgumentCaptor.forClass(JmeSimpleTestV2Event.class);
        Mockito.verify(jmeEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(argumentCaptor.capture());
        JmeSimpleTestV2Event receivedDeclarationCreatedEvent = argumentCaptor.getValue();
        assertEquals("idempotenceId", receivedDeclarationCreatedEvent.getIdentity().getIdempotenceId());
    }

    @Test
    void testConsumeMessageWithCustomDeserializationForKey() {
        BeanReferenceMessageKey messageKey = new BeanReferenceMessageKey("myKey", "myNamespace", "myId");
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId")
                .message("message")
                .build();

        sendSync(JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer.OTHER_TOPIC_NAME, messageKey, message);

        Mockito.verify(jmeDeclarationCreatedEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());
        BeanReferenceMessageKeyV2 lastMessageKey = consumer.getLastMessageKey();
        assertEquals("myId", lastMessageKey.getId());
        assertEquals("myNamespace", lastMessageKey.getNamespace());
        assertEquals("myKey", lastMessageKey.getName());
    }

}