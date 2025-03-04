package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.ClusterNameHeaderInterceptor;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.BufferedMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;

import java.util.Optional;

class FailedConsumerRecord extends ConsumerRecord<Object, Object> {

    private FailedConsumerRecord(SequencedMessage sequencedMessage, Object key, Object value) {
        super(sequencedMessage.getTopic(), 0, 0, 0, TimestampType.NO_TIMESTAMP_TYPE,
                0, 0, key, value, clusterHeader(sequencedMessage), Optional.empty());
    }

    /**
     * Add the cluster name as a header to the record to make sure the {@link ch.admin.bit.jeap.messaging.kafka.errorhandling.ErrorServiceSender}
     * will produce the {@link ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent} to the correct cluster.
     */
    private static Headers clusterHeader(SequencedMessage sequencedMessage) {
        RecordHeaders headers = new RecordHeaders();
        ClusterNameHeaderInterceptor.addClusterName(headers, sequencedMessage.getClusterName());
        return headers;
    }

    static FailedConsumerRecord of(SequencedMessage sequencedMessage, AvroMessageKey key, AvroMessage value) {
        return new FailedConsumerRecord(sequencedMessage, key, value);
    }

    static FailedConsumerRecord of(SequencedMessage sequencedMessage, BufferedMessage bufferedMessage) {
        return new FailedConsumerRecord(sequencedMessage, bufferedMessage.getKey(), bufferedMessage.getValue());
    }

    static FailedConsumerRecord of(SequencedMessage sequencedMessage, DeserializedMessage deserializedMessage) {
        return new FailedConsumerRecord(sequencedMessage, deserializedMessage.key(), deserializedMessage.message());
    }
}
