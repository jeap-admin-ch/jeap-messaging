package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.KafkaConfiguration;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.BufferedMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
class SequentialInboxDeserializer {

    private final KafkaProperties kafkaProperties;
    private final KafkaConfiguration kafkaConfiguration;

    private final Map<String, Deserializer<AvroMessage>> valueDeserializerByClusterName = new ConcurrentHashMap<>();
    private final Map<String, Deserializer<AvroMessageKey>> keyDeserializerByClusterName = new ConcurrentHashMap<>();

    DeserializedMessage deserialize(SequencedMessage sequencedMessage, BufferedMessage bufferedMessage) {
        String topic = sequencedMessage.getTopic();
        String clusterNameOrDefault = getClusterNameOrDefault(sequencedMessage);
        Deserializer<AvroMessage> valueDeserializer = valueDeserializerByClusterName.get(clusterNameOrDefault);
        Deserializer<AvroMessageKey> keyDeserializer = keyDeserializerByClusterName.get(clusterNameOrDefault);

        AvroMessageKey key = Deserializers.deserialize(keyDeserializer, topic, bufferedMessage.getKey(), Collections.emptyList(), true);
        AvroMessage value = Deserializers.deserialize(valueDeserializer, topic, bufferedMessage.getValue(), bufferedMessage.getHeaders(), false);
        return new DeserializedMessage(key, value, topic);
    }

    private String getClusterNameOrDefault(SequencedMessage sequencedMessage) {
        String clusterName = sequencedMessage.getClusterName();
        return valueDeserializerByClusterName.containsKey(clusterName) ? clusterName : kafkaProperties.getDefaultClusterName();
    }

    @PostConstruct
    void init() {
        // Prepare deserializers for all clusters
        kafkaProperties.clusterNames().forEach(this::prepareDeserializers);
    }

    private void prepareDeserializers(String clusterName) {
        ConsumerConfig consumerConfig = new ConsumerConfig(kafkaConfiguration.consumerConfig(clusterName));
        addValueDeserializer(clusterName, consumerConfig);
        addKeyDeserialized(clusterName, consumerConfig);
    }

    @SuppressWarnings("unchecked")
    private void addValueDeserializer(String clusterName, ConsumerConfig consumerConfig) {
        Deserializer<AvroMessage> valueDeserializer = consumerConfig.getConfiguredInstance(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, Deserializer.class);
        // ErrorHandlingDeserializer does not implement Configurable, so we need to configure it explicitly
        valueDeserializer.configure(consumerConfig.originals(), false /* isKey */);
        valueDeserializerByClusterName.put(clusterName, valueDeserializer);
    }

    @SuppressWarnings("unchecked")
    private void addKeyDeserialized(String clusterName, ConsumerConfig consumerConfig) {
        Deserializer<AvroMessageKey> keyDeserializer = consumerConfig.getConfiguredInstance(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, Deserializer.class);
        keyDeserializer.configure(consumerConfig.originals(), true /* isKey */);
        keyDeserializerByClusterName.put(clusterName, keyDeserializer);
    }
}
