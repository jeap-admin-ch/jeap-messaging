package ch.admin.bit.jeap.messaging.kafka.filter;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorHandlingTargetFilterTest {

    private static final String SERVICE_NAME = "test-service";

    private ErrorHandlingTargetFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ErrorHandlingTargetFilter(SERVICE_NAME);
    }

    @Test
    void filter_noFailedServiceHeader_returnsFalse() {
        // Given
        ConsumerRecord<Object, Object> consumerRecord = new ConsumerRecord<>("test-topic", 0, 0L, null, null);

        // When
        boolean result = filter.filter(consumerRecord);

        // Then
        assertFalse(result);
    }

    @Test
    void filter_failedServiceHeaderSameService_returnsFalse() {
        // Given
        ConsumerRecord<Object, Object> consumerRecord = new ConsumerRecord<>("test-topic", 0, 0L, null, null);
        consumerRecord.headers().add("jeap_eh_target_service", SERVICE_NAME.getBytes());

        // When
        boolean result = filter.filter(consumerRecord);

        // Then
        assertFalse(result);
    }

    @Test
    void filter_failedServiceHeaderOtherServiceEhsNameHeaderNotSet_returnsTrue() {
        // Given
        ConsumerRecord<Object, Object> consumerRecord = new ConsumerRecord<>("test-topic", 0, 0L, null, null);
        consumerRecord.headers().add("jeap_eh_target_service", "another-service".getBytes());

        // When
        boolean result = filter.filter(consumerRecord);

        // Then
        assertTrue(result);
    }

    @Test
    void filter_failedServiceHeaderOtherServiceEhsNameHeaderSet_returnsTrue() {
        // Given
        ConsumerRecord<Object, Object> consumerRecord = new ConsumerRecord<>("test-topic", 0, 0L, null, null);
        consumerRecord.headers().add("jeap_eh_target_service", "another-service".getBytes());
        consumerRecord.headers().add("jeap_eh_error_handling_service", "my-error-handling-service".getBytes());

        // When
        boolean result = filter.filter(consumerRecord);

        // Then
        assertTrue(result);
    }

}
