package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventType;
import ch.admin.bit.jeap.messaging.model.MessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultContractsValidatorContractsTests {

    private static final String TEST_APP_NAME = "test-app";
    private static final String TEST_APP_PRODUCED_TYPE = "Produced";
    private static final String TEST_APP_PRODUCED_TYPE_VERSION_1 = "1.0.1";
    private static final String TEST_APP_PRODUCED_TYPE_VERSION_1_TOPIC = "Produced-101";
    private static final String TEST_APP_PRODUCED_TYPE_VERSION_2 = "1.1.0";
    private static final String TEST_APP_PRODUCED_TYPE_VERSION_2_TOPIC = "Produced-110";
    private static final String TEST_APP_CONSUMED_TYPE = "Consumed";
    private static final String TEST_APP_CONSUMED_TYPE_VERSION_1 = "1.0.0";
    private static final String TEST_APP_CONSUMED_TYPE_VERSION_1_TOPIC = "Consumed-100";
    private static final String TEST_APP_CONSUMED_TYPE_VERSION_2 = "2.1.0";
    private static final String TEST_APP_CONSUMED_TYPE_VERSION_2_TOPIC = "Consumed-210";
    private static final String ANY_TOPIC = "whatever-topic";

    private static final String TEST_APP_UNREGISTERED_TYPE = "Unregistered";
    private static final String TEST_APP_UNREGISTERED_TYPE_VERSION = "0.9.1";

    private static final String OTHER_APP_PRODUCED_TYPE = "ProducedOther";
    private static final String OTHER_APP_PRODUCED_TYPE_VERSION = "1.1.0";
    private static final String OTHER_APP_CONSUMED_TYPE = "ConsumedOther";
    private static final String OTHER_APP_CONSUMED_TYPE_VERSION = "2.1.0";

    private final DefaultContractsValidator contractsValidator = new DefaultContractsValidator(TEST_APP_NAME, new DefaultContractsProvider());

   @ParameterizedTest
   @CsvSource({TEST_APP_PRODUCED_TYPE_VERSION_1 + "," + TEST_APP_PRODUCED_TYPE_VERSION_1_TOPIC,
               TEST_APP_PRODUCED_TYPE_VERSION_2 + "," + TEST_APP_PRODUCED_TYPE_VERSION_2_TOPIC})
    void test_whenPublisherContractPresent_thenNoException(String version, String topic) {
       MessageType messageType = AvroDomainEventType.newBuilder()
               .setName(TEST_APP_PRODUCED_TYPE)
               .setVersion(version)
               .build();

       assertThatCode(() -> contractsValidator.ensurePublisherContract(messageType, topic))
               .doesNotThrowAnyException();
   }

    @Test
    void test_whenPublishingSharedEvent_thenNoException() {
        MessageType messageType = AvroDomainEventType.newBuilder()
                .setName("MessageProcessingFailedEvent")
                .setVersion("1.2.3")
                .build();

        assertThatCode(() -> contractsValidator.ensurePublisherContract(messageType, ANY_TOPIC))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({TEST_APP_CONSUMED_TYPE_VERSION_1 + "," + TEST_APP_CONSUMED_TYPE_VERSION_1_TOPIC,
            TEST_APP_CONSUMED_TYPE_VERSION_2 + "," + TEST_APP_CONSUMED_TYPE_VERSION_2_TOPIC})
    void test_whenConsumerContractPresent_thenNoException(String version, String topic) {
        MessageType messageType = AvroDomainEventType.newBuilder()
                .setName(TEST_APP_CONSUMED_TYPE)
                .setVersion(version)
                .build();

        assertThatCode(() -> contractsValidator.ensureConsumerContract(messageType, topic))
                .doesNotThrowAnyException();
    }

    @Test
    void test_whenPublisherContractWrongTopic_thenThrowsException() {
        MessageType messageType = AvroDomainEventType.newBuilder()
                .setName(TEST_APP_PRODUCED_TYPE)
                .setVersion(TEST_APP_PRODUCED_TYPE_VERSION_1)
                .build();

        assertThatThrownBy(() -> contractsValidator.ensurePublisherContract(messageType, TEST_APP_PRODUCED_TYPE_VERSION_2_TOPIC))
                .isInstanceOf(NoContractException.class);
    }

    @Test
    void test_whenPublisherContractTwoVersionsOnSeparateTopic_thenDoesNotThrowException() {
        MessageType messageType1 = AvroDomainEventType.newBuilder()
                .setName(TEST_APP_PRODUCED_TYPE)
                .setVersion(TEST_APP_PRODUCED_TYPE_VERSION_1)
                .build();
        MessageType messageType2 = AvroDomainEventType.newBuilder()
                .setName(TEST_APP_PRODUCED_TYPE)
                .setVersion(TEST_APP_PRODUCED_TYPE_VERSION_2)
                .build();

        assertThatCode(() -> contractsValidator.ensurePublisherContract(messageType1, TEST_APP_PRODUCED_TYPE_VERSION_1_TOPIC))
                .doesNotThrowAnyException();
        assertThatCode(() -> contractsValidator.ensurePublisherContract(messageType2, TEST_APP_PRODUCED_TYPE_VERSION_2_TOPIC))
                .doesNotThrowAnyException();
    }

    @Test
    void test_whenConsumerContractWrongTopic_thenThrowsException() {
        MessageType messageType = AvroDomainEventType.newBuilder()
                .setName(TEST_APP_CONSUMED_TYPE)
                .setVersion(TEST_APP_CONSUMED_TYPE_VERSION_1)
                .build();

        assertThatThrownBy(() -> contractsValidator.ensureConsumerContract(messageType, "different-topic"))
                .isInstanceOf(NoContractException.class);
    }

    @Test
    void test_whenConsumerContractDifferentVersionDifferentTopic_thenDoesNotThrowException() {
        assertThatCode(() -> contractsValidator.ensureConsumerContract(TEST_APP_CONSUMED_TYPE, TEST_APP_CONSUMED_TYPE_VERSION_2_TOPIC))
                .doesNotThrowAnyException();
    }

    @Test
    void test_whenPublisherContractWrongVersion_thenThrowsException() {
        MessageType messageType = AvroDomainEventType.newBuilder()
                .setName(TEST_APP_PRODUCED_TYPE)
                .setVersion("9.9.9")
                .build();

        assertThatThrownBy(() -> contractsValidator.ensurePublisherContract(messageType, TEST_APP_PRODUCED_TYPE_VERSION_1_TOPIC))
                .isInstanceOf(NoContractException.class);
    }

    @Test
    void test_whenConsumerContractDifferentVersion_thenDoesNotThrowException() {
        MessageType messageType = AvroDomainEventType.newBuilder()
                .setName(TEST_APP_CONSUMED_TYPE)
                .setVersion("9.9.9")
                .build();

        assertThatCode(() -> contractsValidator.ensureConsumerContract(messageType, TEST_APP_CONSUMED_TYPE_VERSION_1_TOPIC))
                .doesNotThrowAnyException();
    }

    @Test
    void test_whenProducerContractNotPresent_thenThrowsException() {
        MessageType messageType = AvroDomainEventType.newBuilder()
                .setName(TEST_APP_UNREGISTERED_TYPE)
                .setVersion(TEST_APP_UNREGISTERED_TYPE_VERSION)
                .build();

        assertThatThrownBy(() -> contractsValidator.ensurePublisherContract(messageType, ANY_TOPIC))
                .isInstanceOf(NoContractException.class);
    }

    @Test
    void test_whenConsumerContractNotPresent_thenThrowsException() {
        MessageType messageType = AvroDomainEventType.newBuilder()
                .setName(TEST_APP_UNREGISTERED_TYPE)
                .setVersion(TEST_APP_UNREGISTERED_TYPE_VERSION)
                .build();

        assertThatThrownBy(() -> contractsValidator.ensureConsumerContract(messageType, ANY_TOPIC))
                .isInstanceOf(NoContractException.class);
    }

    @Test
    void test_whenOnlyOtherAppProducerContractPresent_thenThrowsException() {
        MessageType messageType = AvroDomainEventType.newBuilder()
                .setName(OTHER_APP_PRODUCED_TYPE)
                .setVersion(OTHER_APP_PRODUCED_TYPE_VERSION)
                .build();

        assertThatThrownBy(() -> contractsValidator.ensurePublisherContract(messageType, ANY_TOPIC))
                .isInstanceOf(NoContractException.class);
    }

    @Test
    void test_whenOnlyOtherAppConsumerContractPresent_thenThrowsException() {
        MessageType messageType = AvroDomainEventType.newBuilder()
                .setName(OTHER_APP_CONSUMED_TYPE)
                .setVersion(OTHER_APP_CONSUMED_TYPE_VERSION)
                .build();

        assertThatThrownBy(() -> contractsValidator.ensureConsumerContract(messageType, ANY_TOPIC))
                .isInstanceOf(NoContractException.class);
    }

}
