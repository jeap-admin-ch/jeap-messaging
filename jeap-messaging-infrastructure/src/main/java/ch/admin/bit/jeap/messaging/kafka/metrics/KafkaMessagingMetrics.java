package ch.admin.bit.jeap.messaging.kafka.metrics;

public interface KafkaMessagingMetrics {

    void incrementSend(String boostrapServers, String applicationName, String topic, String messageType, String messageTypeVersion, Boolean signatureEnabled);

    void incrementConsume(String boostrapServers, String applicationName, String topic, String messageType, String messageTypeVersion);
}
