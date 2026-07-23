package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(classes = {TestConfig.class,
        // Validate only against v2 contracts
        ContractsValidationConfiguration.class}, properties = {
        // Claim to be jme-messaging-receiverpublisher-service so that we have a contract for publishing a JmeDeclarationEvent in Version 3
        "spring.application.name=jme-messaging-receiverpublisher-service",
        // Suppresses the no-contract log statement on the consumer side, but must not affect producer contract enforcement (JEAP-7293)
        "jeap.messaging.kafka.silentIgnoreWithoutContract=true"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
@DirtiesContext
class PublisherContractSilentIgnoreIT extends PublisherContractsITBase {

    @Test
    void testSendEventWithoutContract_silentIgnoreMustNotDisableContractCheck() {
        super.sendEventWithoutContract();
    }

    @Test
    void testSendEventWithContract() {
        super.sendEventWithContract();
    }
}
