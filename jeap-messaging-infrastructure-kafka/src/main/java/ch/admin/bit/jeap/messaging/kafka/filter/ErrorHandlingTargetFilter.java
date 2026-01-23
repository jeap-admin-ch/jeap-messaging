package ch.admin.bit.jeap.messaging.kafka.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
public class ErrorHandlingTargetFilter implements RecordFilterStrategy<Object, Object> {

    private static final String TARGET_SERVICE_HEADER_NAME = "jeap_eh_target_service";
    private static final String ERROR_HANDLING_SERVICE_HEADER_NAME = "jeap_eh_error_handling_service";

    private final String consumerServiceName;

    @Override
    public boolean filter(ConsumerRecord<Object, Object> consumerRecord) {
        Header header = consumerRecord.headers().lastHeader(TARGET_SERVICE_HEADER_NAME);
        if (header == null) {
            return false;
        }
        String serviceName = toString(header.value());
        if (Objects.equals(consumerServiceName, serviceName)) {
            return false;
        }
        String errorHandlingServiceName = extractHeaderValue(consumerRecord, ERROR_HANDLING_SERVICE_HEADER_NAME);

        log.info("Filtering out message because '{}={}'. Message has been resent by '{}'", TARGET_SERVICE_HEADER_NAME, serviceName, errorHandlingServiceName);
        return true;
    }

    private String extractHeaderValue(ConsumerRecord<Object, Object> consumerRecord, String headerKey) {
        Header errorHandlingServiceNameHeader = consumerRecord.headers().lastHeader(headerKey);
        return errorHandlingServiceNameHeader == null ? "unknown" : toString(errorHandlingServiceNameHeader.value());
    }

    private String toString(byte[] headerValue) {
        return new String(headerValue, StandardCharsets.UTF_8);
    }
}
