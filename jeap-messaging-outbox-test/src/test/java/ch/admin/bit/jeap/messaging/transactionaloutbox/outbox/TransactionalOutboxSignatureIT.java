package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestEvent;
import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestMessageKey;
import io.restassured.RestAssured;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SuppressWarnings("SameParameterValue")
@DirtiesContext
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.endpoint.prometheus.access=unrestricted",
                "management.endpoints.web.exposure.include=*",
                "spring.application.name=jme-messaging-receiverpublisher-outbox-service",
                "jeap.messaging.kafka.exposeMessageKeyToConsumer=true"})
@Slf4j
@ActiveProfiles("test-signing")
class TransactionalOutboxSignatureIT extends KafkaIntegrationTestBase {

    @Autowired
    private KafkaProperties kafkaProperties;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    DeferredMessageRepository deferredMessageRepository;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private TransactionalOutbox transactionalOutbox;
    @SuppressWarnings("unused")
    @MockitoBean
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator
    @MockitoBean
    private TestEventSignatureListener testEventListener;
    @Captor
    private ArgumentCaptor<String> jeapSignArgumentCaptor;
    @Captor
    private ArgumentCaptor<String> jeapSignKeyArgumentCaptor;
    @Captor
    private ArgumentCaptor<String> jeapCertArgumentCaptor;
    @Autowired
    DeferredMessageTestUtil deferredMessageTestUtil;
    @LocalServerPort
    int localServerPort;


    @AfterEach
    void cleanUp() {
        deferredMessageTestUtil.deleteAllMessages();
    }

    @Transactional
    @Commit
    @Test
    void testOutboxSend() {
        final String idempotenceIdEvent1 = "idempotenceId1";
        final String idempotenceIdEvent2 = "idempotenceId2";
        final TestEvent testEvent1 = TestEventBuilder.create().idempotenceId(idempotenceIdEvent1).build();
        final TestEvent testEvent2 = TestEventBuilder.create().idempotenceId(idempotenceIdEvent2).build();
        final TestMessageKey testMessageKey = TestMessageKey.newBuilder().setSomeProperty("someValue").build();

        transactionalOutbox.sendMessage(testEvent1, testMessageKey, TestEventConsumer.TOPIC);
        transactionalOutbox.sendMessageScheduled(testEvent2, TestEventConsumer.TOPIC);

        TestTransaction.end();
        verify(testEventListener, timeout(TEST_TIMEOUT).times(2)).receive(any(), any(), jeapSignArgumentCaptor.capture(), jeapSignKeyArgumentCaptor.capture(), jeapCertArgumentCaptor.capture());
        final List<String> receivedSignatures = jeapSignArgumentCaptor.getAllValues();
        assertEquals(2, receivedSignatures.size());
        final List<String> receivedSignatureKeys = jeapSignKeyArgumentCaptor.getAllValues();
        assertEquals(1, receivedSignatureKeys.stream().filter(Objects::nonNull).collect(Collectors.toList()).size());
        assertEquals(1, receivedSignatureKeys.stream().filter(Objects::isNull).collect(Collectors.toList()).size());
        final List<String> receivedCerts = jeapCertArgumentCaptor.getAllValues();
        assertEquals(2, receivedCerts.size());
    }

    @SneakyThrows
    @Transactional
    @Commit
    @Test
    void testOutboxMetrics() {
        String bootstrapServer = kafkaProperties.getBootstrapServers(KafkaProperties.DEFAULT_CLUSTER);
        final TestEvent testEvent1 = TestEventBuilder.create().idempotenceId("idempotenceId1").build();
        final TestEvent testEvent2 = TestEventBuilder.create().idempotenceId("idempotenceId2").build();
        final TestEvent testEvent3 = TestEventBuilder.create().idempotenceId("idempotenceId3").build();
        transactionalOutbox.sendMessage(testEvent1, TestEventConsumer.TOPIC);
        transactionalOutbox.sendMessageScheduled(testEvent2, TestEventConsumer.TOPIC);
        transactionalOutbox.sendMessage(testEvent3, TestEventConsumer.TOPIC);
        TestTransaction.end();
        deferredMessageTestUtil.waitTillAllMessagesProcessed();

        final String metrics = RestAssured.given().port(localServerPort).get("/actuator/prometheus").getBody().asString();
        assertThat(metrics).contains("jeap_messaging_signature_certificate_days_remaining{application=\"jme-messaging-receiverpublisher-outbox-service\"}");
        assertThat(metrics).contains("jeap_messaging_total{application=\"jme-messaging-receiverpublisher-outbox-service\",bootstrapservers=\"" + bootstrapServer + "\",message=\"TestEvent\",signed=\"1\",topic=\"test-topic\",type=\"producer\",version=\"na\"}");
    }

}
