package ch.admin.bit.jeap.messaging.kafka.test.integration.filter;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.MessagingMessageConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.MessagingMessageListener;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@AutoConfigureObservability
@SpringBootTest(classes = {TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=jme-messaging-receiverpublisher-service",
                "management.endpoint.prometheus.access=unrestricted",
                "management.endpoints.web.exposure.include=*"
        })
@Import(MessagingMessageConsumer.class)
@DirtiesContext
class EHSFilterIT extends KafkaIntegrationTestBase {

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @MockitoBean
    @SuppressWarnings("unused")
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator

    //Register a Messaging Message listener, we need this to verify the messages
    @MockitoBean
    @SuppressWarnings("unused")
    private MessagingMessageListener messagingMessageListener;

    @Test
    void sendMessage_doNotReceiveFiltered() {
        JmeDeclarationCreatedEvent message1 = createMessage("idempotenceId1", "gugu1");
        JmeDeclarationCreatedEvent message2 = createMessage("idempotenceId2", "gugu2");
        JmeDeclarationCreatedEvent message3 = createMessage("idempotenceId3", "gugu3");
        JmeDeclarationCreatedEvent message4 = createMessage("idempotenceId4", "gugu4");
        JmeDeclarationCreatedEvent message5 = createMessage("idempotenceId5", "gugu5");

        Header headerEHS = new RecordHeader("jeap_eh_error_handling_service", "test-error-handling-service".getBytes());
        Header headerOtherService1 = new RecordHeader("jeap_eh_target_service", "other-service-1".getBytes());
        Header headerSameService = new RecordHeader("jeap_eh_target_service", "jme-messaging-receiverpublisher-service".getBytes());
        Header headerOtherService2 = new RecordHeader("jeap_eh_target_service", "other-service-2".getBytes());

        sendSyncWithHeaders(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message1); // first message without headers
        sendSyncWithHeaders(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message2, headerOtherService1); // second message with header other service, without ehs header
        sendSyncWithHeaders(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message3, headerSameService, headerEHS); // second message with header same service
        sendSyncWithHeaders(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message4, headerOtherService2, headerEHS); // second message with header same service
        sendSyncWithHeaders(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message5);  // last message without headers

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(messagingMessageListener, Mockito.timeout(TEST_TIMEOUT).times(3))
                .receive(captor.capture());

        List<Message> capturedMessages = captor.getAllValues();
        for (Message message : capturedMessages) {
            JmeDeclarationCreatedEvent payload = (JmeDeclarationCreatedEvent) message.getPayload();
            String messageString = payload.getPayload().getMessage();
            assertTrue(messageString.equals("gugu1")
                    || messageString.equals("gugu3")
                    || messageString.equals("gugu5"));
        }
    }

    @Test
    void sendFilteredBatch_offsetAdvances() throws Exception {
        Header headerEHS = new RecordHeader("jeap_eh_error_handling_service", "test-error-handling-service".getBytes());
        Header headerOtherService = new RecordHeader("jeap_eh_target_service", "other-service".getBytes());

        OffsetAndMetadata currentOffsetBefore = KafkaTestUtils.getCurrentOffset(embeddedKafka.getBrokersAsString(), "messaging-test-group", JmeDeclarationCreatedEventConsumer.TOPIC_NAME, 0);

        JmeDeclarationCreatedEvent filtered1 = createMessage("filtered1", "filtered1");
        JmeDeclarationCreatedEvent filtered2 = createMessage("filtered2", "filtered2");
        JmeDeclarationCreatedEvent filtered3 = createMessage("filtered3", "filtered3");

        // These should all be filtered
        sendSyncWithHeaders(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, filtered1, headerOtherService, headerEHS);
        sendSyncWithHeaders(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, filtered2, headerOtherService, headerEHS);
        sendSyncWithHeaders(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, filtered3, headerOtherService, headerEHS);

        // await until offset has advanced by 3
        await()
                .atMost(2, TimeUnit.SECONDS)
                .until(() -> {
                    OffsetAndMetadata currentOffsetAfter = KafkaTestUtils.getCurrentOffset(embeddedKafka.getBrokersAsString(), "messaging-test-group", JmeDeclarationCreatedEventConsumer.TOPIC_NAME, 0);
                    return currentOffsetAfter.offset() >= currentOffsetBefore.offset() + 3;
                });
    }

    private JmeDeclarationCreatedEvent createMessage(String idempotenceId, String messageContent) {
        return JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(idempotenceId)
                .message(messageContent)
                .build();
    }
}
