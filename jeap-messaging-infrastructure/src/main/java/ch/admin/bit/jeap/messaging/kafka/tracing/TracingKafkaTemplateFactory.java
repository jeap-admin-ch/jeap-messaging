package ch.admin.bit.jeap.messaging.kafka.tracing;

import brave.messaging.MessagingTracing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class TracingKafkaTemplateFactory {

    private final MessagingTracing messagingTracing;

    public <K,V> KafkaTemplate<K,V> createKafkaTemplate(Map<String, Object> configs, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
        KafkaTemplate<K, V> kafkaTemplate = new KafkaTemplate<>(new TracingKafkaProducerFactory<>(messagingTracing, configs, keySerializer, valueSerializer));
        kafkaTemplate.setObservationEnabled(true);
        return kafkaTemplate;
    }

}
