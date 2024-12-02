package ch.admin.bit.jeap.messaging.kafka.metrics;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.MessageVersionAccessor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;

public class ProducerMetricsInterceptor implements ProducerInterceptor<Object, Object> {

    public static final String METER_REGISTRY = "producerMetricsInterceptor.meterRegistry";
    public static final String APPLICATION_NAME = "producerMetricsInterceptor.applicationName";

    private KafkaMessagingMetrics kafkaMessagingMetrics;

    private String applicationName;
    private String boostrapServers;

    @Override
    public void configure(Map<String, ?> configs) {
        kafkaMessagingMetrics = (KafkaMessagingMetrics) configs.get(METER_REGISTRY);
        applicationName = (String) configs.get(APPLICATION_NAME);
        boostrapServers = (String) configs.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG);
    }

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        String type;
        String version;
        if (record.value() instanceof AvroMessage avroMessage) {
            type = avroMessage.getType().getName();
            version = MessageVersionAccessor.getGeneratedVersion(avroMessage.getClass());
        } else {
            type = record.value().getClass().getSimpleName();
            version = "na";
        }
        kafkaMessagingMetrics.incrementSend(boostrapServers, applicationName, record.topic(), type, version);
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // Nothing to do.
    }

    @Override
    public void close() {
        // Nothing to clean-up on close().
    }
}
