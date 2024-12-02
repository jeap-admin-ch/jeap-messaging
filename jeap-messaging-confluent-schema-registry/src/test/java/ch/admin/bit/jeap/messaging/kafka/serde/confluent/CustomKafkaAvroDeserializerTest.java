package ch.admin.bit.jeap.messaging.kafka.serde.confluent;

import ch.admin.bit.jeap.kafka.examples.Payment;
import ch.admin.bit.jeap.kafka.examples.PaymentV2;
import ch.admin.bit.jeap.kafka.serde.confluent.AbstractConfluentSerdeTestBase;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.config.CustomKafkaAvroDeserializerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomKafkaAvroDeserializerTest extends AbstractConfluentSerdeTestBase {

    @Test
    void deserializeValueWithCustomTargetType() {
        Serializer<Object> serializer = kafkaAvroSerdeProvider.getValueSerializer();
        Payment payment = new Payment("id-123", "vor", "nach", 303.808d);
        byte[] serializedRecord = serializer.serialize("test-topic", payment);

        // Now we will deserialize the value using a different but compatible target class
        CustomKafkaAvroDeserializer customKafkaAvroDeserializer = setupCustomKafkaAvroDeserializerForValueWithTargetType(PaymentV2.class);
        PaymentV2 deserializedPayment = (PaymentV2) customKafkaAvroDeserializer.deserialize("test-topic", serializedRecord);

        assertEquals("id-123", deserializedPayment.getId().toString());
        assertEquals("vor", deserializedPayment.getVorname().toString());
        assertEquals("nach", deserializedPayment.getNachname().toString());
        assertEquals(303.808d, deserializedPayment.getValue());
    }

    @Test
    void deserializeKeyWithCustomTargetType() {
        Serializer<Object> serializer = kafkaAvroSerdeProvider.getValueSerializer();
        Payment payment = new Payment("id-123", "vor", "nach", 303.808d);
        byte[] serializedRecord = serializer.serialize("test-topic", payment);

        // Now we will deserialize using a different but compatible target class
        CustomKafkaAvroDeserializer customKafkaAvroDeserializer = setupCustomKafkaAvroDeserializerForKeyWithTargetType(PaymentV2.class);
        PaymentV2 deserializedPayment = (PaymentV2) customKafkaAvroDeserializer.deserialize("test-topic", serializedRecord);

        assertEquals("id-123", deserializedPayment.getId().toString());
        assertEquals("vor", deserializedPayment.getVorname().toString());
        assertEquals("nach", deserializedPayment.getNachname().toString());
        assertEquals(303.808d, deserializedPayment.getValue());
    }

    private CustomKafkaAvroDeserializer setupCustomKafkaAvroDeserializerForValueWithTargetType(Class<?> targetType) {
        CustomKafkaAvroDeserializer customKafkaAvroDeserializer = new CustomKafkaAvroDeserializer();
        Map<String, Object> deserializerProperties = kafkaAvroSerdeProvider.getSerdeProperties().avroDeserializerProperties("someCluster");

        deserializerProperties.put(CustomKafkaAvroDeserializerConfig.SPECIFIC_AVRO_VALUE_TYPE, targetType.getName());
        customKafkaAvroDeserializer.configure(deserializerProperties, false);

        return customKafkaAvroDeserializer;
    }

    private CustomKafkaAvroDeserializer setupCustomKafkaAvroDeserializerForKeyWithTargetType(Class<?> targetType) {
        CustomKafkaAvroDeserializer customKafkaAvroDeserializer = new CustomKafkaAvroDeserializer();
        Map<String, Object> deserializerProperties = kafkaAvroSerdeProvider.getSerdeProperties().avroDeserializerProperties("someCluster");

        deserializerProperties.put(CustomKafkaAvroDeserializerConfig.SPECIFIC_AVRO_KEY_TYPE, targetType.getName());
        customKafkaAvroDeserializer.configure(deserializerProperties, true);

        return customKafkaAvroDeserializer;
    }

}