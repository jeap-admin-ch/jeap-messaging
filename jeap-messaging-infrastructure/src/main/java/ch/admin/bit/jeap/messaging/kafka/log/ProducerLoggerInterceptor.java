package ch.admin.bit.jeap.messaging.kafka.log;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;

import static ch.admin.bit.jeap.messaging.kafka.log.TopicLogger.topic;
import static net.logstash.logback.argument.StructuredArguments.value;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
public class ProducerLoggerInterceptor implements ProducerInterceptor<Object, Object> {
    public static final String CLUSTER_NAME_CONFIG = "clusternameheaderinterceptor.clustername";
    private String clusterName;

    @Override
    public void configure(Map<String, ?> configs) {
        String clusterName = (String) configs.get(CLUSTER_NAME_CONFIG);
        if (!hasText(clusterName)) {
            throw new IllegalStateException("Mandatory config property %s is missing".formatted(CLUSTER_NAME_CONFIG));
        }
        this.clusterName = clusterName;
    }

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        if (log.isInfoEnabled()) {
            log.info("Published {} to {} using cluster {}", MessageLogger.message(record.value()), topic(record), clusterName);
        }
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (exception == null) {
            log.debug("Kafka acknowledged offset {} on {}", value("offset", metadata.offset()), topic(metadata));
        } else {
            log.error("Publishing events on {} failed with {}", topic(metadata), value("exception", exception.getClass()), exception);
        }
    }

    @Override
    public void close() {
        // Nothing to clean-up on close().
    }
}
