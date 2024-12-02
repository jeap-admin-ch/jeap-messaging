package ch.admin.bit.jeap.messaging.transactionaloutbox.messaging;

import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.MessageSerializer;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutboxException;
import org.apache.kafka.common.serialization.Serializer;

class KafkaAvroMessageSerializer implements MessageSerializer {

    private final Serializer<Object> valueSerializer;
    private final Serializer<Object> keySerializer;

    KafkaAvroMessageSerializer(KafkaAvroSerdeProvider kafkaAvroSerdeProvider) {
        this.valueSerializer = kafkaAvroSerdeProvider.getValueSerializer();
        this.keySerializer = kafkaAvroSerdeProvider.getKeySerializer();
    }

    @Override
    public byte[] serializeMessage(Message message, String topic) {
        try {
            return valueSerializer.serialize(topic, message);
        } catch (Exception e) {
            throw TransactionalOutboxException.messageSerializationFailed(message, topic, e);
        }
    }

    @Override
    public byte[] serializeKey(Object key, String topic) {
        try {
            return keySerializer.serialize(topic, key);
        } catch (Exception e) {
            throw TransactionalOutboxException.messageKeySerializationFailed(key, topic, e);
        }
    }
}
