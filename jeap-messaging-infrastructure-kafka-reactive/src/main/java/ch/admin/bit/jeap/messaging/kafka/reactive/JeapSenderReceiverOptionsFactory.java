package ch.admin.bit.jeap.messaging.kafka.reactive;

import ch.admin.bit.jeap.messaging.kafka.KafkaConfiguration;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class JeapSenderReceiverOptionsFactory {

    final String clusterName;
    final KafkaProperties springKafkaProperties;
    final KafkaConfiguration jeapKafkaConfiguration;
    final KafkaAvroSerdeProvider kafkaAvroSerdeProvider;

    public <K,V> SenderOptions<K,V> createSenderOptions() {
        Map<String, Object> producerConfig = new HashMap<>(springKafkaProperties.buildProducerProperties());
        producerConfig.putAll(jeapKafkaConfiguration.producerConfig(clusterName));
        SenderOptions<K,V> senderOptions = SenderOptions.create(producerConfig);
        //noinspection unchecked
        return senderOptions
                .withKeySerializer((Serializer<K>) kafkaAvroSerdeProvider.getKeySerializer())
                .withValueSerializer((Serializer<V>) kafkaAvroSerdeProvider.getValueSerializer());
    }

    public <K,V> ReceiverOptions<K,V> createReceiverOptions() {
        Map<String, Object> consumerConfig = new HashMap<>(springKafkaProperties.buildConsumerProperties());
        consumerConfig.putAll(jeapKafkaConfiguration.consumerConfig(clusterName));
        //noinspection unchecked
        return ReceiverOptions.create(consumerConfig);
    }


    // additional factory methods for convenience

    public <K,V> ReceiverOptions<K,V> createReceiverOptions(String... topics) {
        ReceiverOptions<K,V> receiverOptions = createReceiverOptions();
        return receiverOptions.subscription(Arrays.asList(topics));
    }

}
