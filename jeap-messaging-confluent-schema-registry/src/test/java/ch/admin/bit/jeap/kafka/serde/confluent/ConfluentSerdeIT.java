package ch.admin.bit.jeap.kafka.serde.confluent;

import ch.admin.bit.jeap.kafka.examples.Payment;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfluentSerdeIT extends AbstractConfluentSerdeTestBase {

    @Test
    void testSerdeRoundtrip(@Autowired @Qualifier("valueDeserializer") Deserializer<Object> deserializer) {
        Serializer<Object> serializer = kafkaAvroSerdeProvider.getValueSerializer();
        Payment payment = new Payment("id-123", "vor", "nach", 303.808d);

        byte[] serializedRecord = serializer.serialize("test-topic", payment);
        Payment deserializedPayment = (Payment) deserializer.deserialize("test-topic", serializedRecord);

        assertThat(deserializedPayment)
                .isEqualTo(payment);
    }

    @Test
    void testSerdeRoundtripWithGenericDeserializer() {
        Serializer<Object> serializer = kafkaAvroSerdeProvider.getValueSerializer();
        Deserializer<GenericData.Record> deserializer = kafkaAvroSerdeProvider.getGenericDataRecordDeserializerWithoutSignatureCheck();
        Payment payment = new Payment("id-123", "vor", "nach", 303.808d);

        byte[] serializedRecord = serializer.serialize("test-topic", payment);
        GenericData.Record deserializedPayment = deserializer.deserialize("test-topic", serializedRecord);

        assertThat(deserializedPayment.get("id").toString())
                .isEqualTo("id-123");
    }
}
