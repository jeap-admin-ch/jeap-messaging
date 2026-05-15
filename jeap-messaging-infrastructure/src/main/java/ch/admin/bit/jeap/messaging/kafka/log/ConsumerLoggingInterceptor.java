package ch.admin.bit.jeap.messaging.kafka.log;

import ch.admin.bit.jeap.messaging.kafka.tracing.TracerBridge;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;

import static ch.admin.bit.jeap.messaging.kafka.log.TopicLogger.topic;
import static net.logstash.logback.argument.StructuredArguments.value;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
public class ConsumerLoggingInterceptor implements ConsumerInterceptor<Object, Object> {

    public static final String TRACER_BRIDGE = "consumerlogginginterceptor.tracerbridge";
    public static final String CLUSTER_NAME_CONFIG = "consumerlogginginterceptor.clustername";

    private TracerBridge tracerBridge = TracerBridge.NOOP;
    private String clusterName;

    @Override
    public void configure(Map<String, ?> configs) {
        if (configs.containsKey(TRACER_BRIDGE)) {
            tracerBridge = (TracerBridge) configs.get(TRACER_BRIDGE);
        }
        String clusterName = (String) configs.get(CLUSTER_NAME_CONFIG);
        if (!hasText(clusterName)) {
            throw new IllegalStateException("Mandatory config property %s is missing".formatted(CLUSTER_NAME_CONFIG));
        }
        this.clusterName = clusterName;

    }

    @Override
    public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
        records.forEach(this::onConsume);
        return records;
    }

    private void onConsume(ConsumerRecord<Object, Object> record) {
        // Spring Kafka's listener-level Observation only establishes a span later, when the record is handed to the
        // listener method. This interceptor runs earlier in the consumer pipeline, so we establish the span ourselves
        // by extracting the trace context from the record's propagation headers to correlate the log line below.
        try (TracerBridge.Scope _ = tracerBridge.getSpan(record, "jeap-messaging.logging-interceptor")) {
            if (log.isInfoEnabled()) {
                log.info("Received {} from {} with offset {} on cluster {}",
                        MessageLogger.message(record.value()), topic(record), record.offset(), clusterName);
            }
        }
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        offsets.forEach(this::onCommit);
    }

    private void onCommit(TopicPartition topic, OffsetAndMetadata offset) {
        if (offset.metadata() == null) {
            log.debug("Commit offset {} on {}", value("offset", offset.offset()), topic(topic));
        } else {
            log.debug("Commit offset {} on {} with {}", value("offset", offset.offset()), topic(topic), value("metadata", offset.metadata()));
        }
    }

    @Override
    public void close() {
        // Nothing to clean-up on close().
    }
}
