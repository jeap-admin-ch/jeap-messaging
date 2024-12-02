package ch.admin.bit.jeap.messaging.kafka.tracing;

import brave.kafka.clients.KafkaTracing;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;

@RequiredArgsConstructor
public class JeapKafkaTracing {

    private final KafkaTracing kafkaTracing;

    public <K, V> Consumer<K, V> consumer(Consumer<K, V> consumer) {
        return kafkaTracing.consumer(consumer);
    }
}
