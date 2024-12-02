package ch.admin.bit.jeap.messaging.kafka.metrics;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.MessageVersionAccessor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;

public class ConsumerMetricsInterceptor implements ConsumerInterceptor<Object, Object> {

    public static final String METER_REGISTRY = "consumerMetricsInterceptor.meterRegistry";
    public static final String APPLICATION_NAME = "consumerMetricsInterceptor.applicationName";

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
    public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
        records.forEach(this::onConsume);
        return records;
    }

    private void onConsume(ConsumerRecord<Object, Object> message) {
        String type;
        String version;
        if (message.value() instanceof AvroMessage avroMessage) {
            type = avroMessage.getType().getName();
            version = MessageVersionAccessor.getGeneratedVersion(avroMessage.getClass());
        } else {
            type = message.value().getClass().getSimpleName();
            version = "na";
        }
        kafkaMessagingMetrics.incrementConsume(boostrapServers, applicationName, message.topic(), type, version);
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // Nothing to do.
    }

    @Override
    public void close() {
        // Nothing to clean-up on close().
    }
}
