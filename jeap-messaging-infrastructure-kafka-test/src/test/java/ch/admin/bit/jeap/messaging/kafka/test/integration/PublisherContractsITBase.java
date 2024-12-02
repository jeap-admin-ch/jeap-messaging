package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.kafka.contract.NoContractException;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestEventConsumer;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;

@SuppressWarnings("java:S5778") // Test lambda throwing with 2+ invocations throwing exceptions - fine here
class PublisherContractsITBase extends KafkaIntegrationTestBase {

    void sendEventWithoutContract() {
        AvroMessage message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").message("message").build();
        Assertions.assertThrows(NoContractException.class, () -> kafkaTemplate.send(TestEventConsumer.TOPIC_NAME, message).get());
    }

    @SneakyThrows
    void sendEventWithContract() {
        AvroMessage message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId")
                .message("sendEventWithContract")
                .build();
        Assertions.assertDoesNotThrow(() -> kafkaTemplate.send(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, message).get());
    }
}
