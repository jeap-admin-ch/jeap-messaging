package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.contract.DefaultContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.contract.NoContractException;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventOtherTopicConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {TestConfig.class,
        // Validate only against v2 contracts
        ContractsValidationConfiguration.class},
        // Claim to be jme-messaging-receiverpublisher-service so that we have a contract for publishing a JmeDeclarationEvent in Version 3
        properties = {"spring.application.name=jme-messaging-receiverpublisher-service"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
@DirtiesContext
// Inherit tests from PublisherContractsITBase and execute them based only v2 contracts only
class PublisherContractsIT extends PublisherContractsITBase {

    @Test
    void assertContractValidator(@Autowired ContractsValidator contractsValidator) {
        assertThat(contractsValidator).isInstanceOf(DefaultContractsValidator.class);
    }

    @Test
    void testSendEventToTopicWithoutContract() {
        AvroMessage message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId")
                .message("sendEventToTopicWithoutContract")
                .build();
        Assertions.assertThrows(NoContractException.class, () -> kafkaTemplate.send(JmeDeclarationCreatedEventOtherTopicConsumer.TOPIC_NAME, message).get());
    }

    @Test
    void testSendEventWithoutContract() {
        super.sendEventWithoutContract();
    }

    @Test
    void testSendEventWithContract() {
        super.sendEventWithContract();
    }

}
