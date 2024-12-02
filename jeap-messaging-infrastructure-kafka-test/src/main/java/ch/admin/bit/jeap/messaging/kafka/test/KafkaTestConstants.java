package ch.admin.bit.jeap.messaging.kafka.test;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.kafka.contract.ConsumerContractInterceptor;
import ch.admin.bit.jeap.messaging.kafka.contract.ProducerContractInterceptor;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;

public final class KafkaTestConstants {
    private KafkaTestConstants() {
    }

    /**
     * Property that forces the {@link ConsumerContractInterceptor} to skip the consumer contract check for a consumer
     */
    public static final String TEST_CONSUMER_DISABLE_CONTRACT_CHECK_PROPERTY =
            ConsumerContractInterceptor.EXEMPT_FROM_CONSUMER_CONTRACT_CHECK + "=true";

    /**
     * Header that forces the {@link ProducerContractInterceptor} to skip the contract check for a record
     *
     * @see KafkaIntegrationTestBase#sendSync(String, AvroMessage)
     */
    public static final Header TEST_PRODUCER_DISABLE_CONTRACT_CHECK_HEADER = new RecordHeader(
            ProducerContractInterceptor.EXEMPT_FROM_PRODUCER_CONTRACT_CHECK_HEADER, "true".getBytes(StandardCharsets.UTF_8));
}
