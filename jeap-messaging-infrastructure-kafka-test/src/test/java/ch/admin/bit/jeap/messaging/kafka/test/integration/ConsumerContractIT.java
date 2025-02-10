package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.contract.DefaultContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventOtherTopicConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {TestConfig.class,
        // Validate only against v2 contracts
        ContractsValidationConfiguration.class}, properties = {
        // Claim to be jme-messaging-subscriber-service so that we have a contract to receive JmeDeclarationEvent in Version 3.
        "spring.application.name=jme-messaging-subscriber-service"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
@DirtiesContext
// Inherit tests from ConsumerContractITBase and execute them based on v2 contracts only
class ConsumerContractIT extends KafkaIntegrationTestBase {

    @MockitoBean
    private MessageListener<JmeDeclarationCreatedEvent> jmeEventProcessor;


    @Test
    void assertContractValidator(@Autowired ContractsValidator contractsValidator) {
        assertThat(contractsValidator).isInstanceOf(DefaultContractsValidator.class);
    }

    @Test
    @SneakyThrows
    void testConsumeEventOnTopicWithoutContract(@Autowired JmeDeclarationCreatedEventOtherTopicConsumer consumer) {
        // Publish an JmeDeclarationCreatedEvent to a topic for which the application does not have a contract
        // -> the consumer listening on that topic should not receive the event
        AvroDomainEvent event = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId").message("eventSentOnTopicWithoutContract").build();
        sendSync(JmeDeclarationCreatedEventOtherTopicConsumer.TOPIC_NAME, event);

        // Wait a little for the event to get consumed (which it should not in this test)
        Thread.sleep(100L);
        assertThat(consumer.hasConsumed()).isFalse();
    }

    @Test
    void testConsumeEventWithContract() {
        //Has consumer contract -> listener must be executed
        JmeDeclarationCreatedEvent message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId").message("consumeEventWithContract")
                .build();
        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message);

        Mockito.verify(jmeEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());
    }
}