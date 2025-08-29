package ch.admin.bit.jeap.messaging.kafka.serde;

import lombok.Value;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

/**
 * There is a problem with exposing KafkaAvroSerializer/Deserializer as a Spring bean.
 * See <a href="https://github.com/confluentinc/schema-registry/issues/553">schema-registry issue 553</a>
 */
@Value
public class KafkaAvroSerdeProvider {

    Serializer<Object> valueSerializer;
    Serializer<Object> keySerializer;
    /**
     * A deserializer that does not instantiate specific java avro binding instances, but {@link GenericData.Record}
     * instead (essentially, a key/value representation of the avro object). This is useful for clients that do not
     * have the java bindings on the classpath, such as the jEAP Error Handling Service. The deserializer provided
     * here does not support any jEAP-specific features such as error handling or decryption.
     */
    Deserializer<GenericData.Record> genericDataRecordDeserializerWithoutSignatureCheck;
    KafkaAvroSerdeProperties serdeProperties;
}
