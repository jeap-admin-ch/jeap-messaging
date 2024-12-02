package ch.admin.bit.jeap.messaging.kafka.tracing;

import brave.kafka.clients.KafkaTracing;
import brave.messaging.MessagingTracing;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.lang.Nullable;

import java.util.Map;

public class TracingKafkaProducerFactory<K, V> extends DefaultKafkaProducerFactory<K, V> {

    private final MessagingTracing messagingTracing;

    public TracingKafkaProducerFactory(MessagingTracing messagingTracing, Map<String, Object> configs) {
        super(configs);
        this.messagingTracing = messagingTracing;
    }

    public TracingKafkaProducerFactory(MessagingTracing messagingTracing, Map<String, Object> configs, @Nullable Serializer<K> keySerializer, @Nullable Serializer<V> valueSerializer) {
        super(configs, keySerializer, valueSerializer);
        this.messagingTracing = messagingTracing;
    }

    @Override
    public Producer<K, V> createProducer() {
        return KafkaTracing.create(messagingTracing).producer(super.createProducer());
    }
}
