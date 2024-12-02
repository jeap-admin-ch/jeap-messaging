package ch.admin.bit.jeap.messaging.kafka.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KafkaMeterRegistryMetrics implements KafkaMessagingMetrics {

    private final MeterRegistry meterRegistry;

    private static final String METRIC_NAME = "jeap_messaging";
    private static final String TAG_BOOTSTRAPSERVERS = "bootstrapservers";
    private static final String TAG_APPLICATION = "application";
    private static final String TAG_TYPE = "type";
    private static final String TAG_MESSAGE = "message";
    private static final String TAG_TOPIC = "topic";
    private static final String TAG_VERSION = "version";
    private static final String NOT_AVAILABLE = "na";
    private static final String PRODUCER = "producer";
    private static final String CONSUMER = "consumer";

    @Override
    public void incrementSend(String boostrapServers, String applicationName, String topic, String messageType, String messageTypeVersion) {
        meterRegistry.counter(METRIC_NAME, getTags(boostrapServers, applicationName, topic, messageType, messageTypeVersion, PRODUCER)).increment();
    }

    @Override
    public void incrementConsume(String boostrapServers, String applicationName, String topic, String messageType, String messageTypeVersion) {
        meterRegistry.counter(METRIC_NAME, getTags(boostrapServers, applicationName, topic, messageType, messageTypeVersion, CONSUMER)).increment();
    }

    private Tags getTags(String bootstrapServers, String applicationName, String topic, String messageType, String messageTypeVersion, String type) {
        if (bootstrapServers == null) {
            bootstrapServers = NOT_AVAILABLE;
        }
        if (messageTypeVersion == null) {
            messageTypeVersion = NOT_AVAILABLE;
        }
        return Tags.of(
                TAG_BOOTSTRAPSERVERS, bootstrapServers,
                TAG_APPLICATION, applicationName,
                TAG_TOPIC, topic,
                TAG_MESSAGE, messageType,
                TAG_TYPE, type,
                TAG_VERSION, messageTypeVersion
        );
    }
}
