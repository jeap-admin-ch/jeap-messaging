package ch.admin.bit.jeap.messaging.kafka.serde;

import java.util.Map;

public interface KafkaAvroSerdeProperties {
    Map<String, Object> avroSerializerProperties(String clusterName);

    Map<String, Object> avroDeserializerProperties(String clusterName);
}
