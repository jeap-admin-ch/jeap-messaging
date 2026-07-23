package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventOtherTopicConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = {TestConfig.class,
        // Validate only against v2 contracts
        ContractsValidationConfiguration.class}, properties = {
        // Claim to be jme-messaging-subscriber-service so that we have a contract to receive JmeDeclarationEvent in Version 3.
        "spring.application.name=jme-messaging-subscriber-service",
        // Suppresses the no-contract log statement, but must NOT disable contract enforcement (JEAP-7293)
        "jeap.messaging.kafka.silentIgnoreWithoutContract=true"})
@DirtiesContext
class ConsumerContractSilentIgnoreIT extends KafkaIntegrationTestBase {

    @MockitoBean
    private MessageListener<JmeDeclarationCreatedEvent> jmeEventProcessor;

    @Test
    void testConsumeEventOnTopicWithoutContract_silentIgnoreMustNotDisableContractCheck(
            @Autowired JmeDeclarationCreatedEventOtherTopicConsumer consumer) {
        // Publish an JmeDeclarationCreatedEvent to a topic for which the application does not have a contract
        // -> the consumer listening on that topic should not receive the event, even with silentIgnoreWithoutContract on
        AvroDomainEvent event = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId").message("eventSentOnTopicWithoutContract").build();
        sendSync(JmeDeclarationCreatedEventOtherTopicConsumer.TOPIC_NAME, event);

        // Send a contract-covered sentinel event and wait for it to be consumed, proving the application
        // was consuming the whole time and did not just miss the filtered event
        JmeDeclarationCreatedEvent sentinel = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId").message("sentinelEventWithContract")
                .build();
        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, sentinel);
        Mockito.verify(jmeEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());

        // The event without contract must not get consumed within the observation period
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2))
                .until(() -> !consumer.hasConsumed());
    }

    @Test
    void testConsumeEventWithContract() {
        // Has consumer contract -> listener must be executed, also with silentIgnoreWithoutContract on
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId").message("consumeEventWithContract")
                .build();
        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message);

        Mockito.verify(jmeEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());
    }
}
