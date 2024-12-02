package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestEventConsumer;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = TestConfig.class, properties = {
        "spring.application.name=EmbeddedKafkaPropertyPrecedenceIT"})
@DirtiesContext
@ActiveProfiles("localkafkaprops")
class EmbeddedKafkaPropertyPrecedenceIT extends KafkaIntegrationTestBase {

    @MockBean
    private MessageListener<JmeDeclarationCreatedEvent> testEventProcessor;

    @Test
    void publishConsumeUsingEmbeddedKafka() {
        // Makes sure the event can be produced/consumed using embedded kafka, even if a kafka broker is configured in
        // application-localkafkaprops.yml
        AvroMessage message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").message("message").build();
        sendSync(TestEventConsumer.TOPIC_NAME, message);
        Mockito.verify(testEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());
    }
}
