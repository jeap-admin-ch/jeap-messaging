package ch.admin.bit.jeap.messaging.kafka.log;

import com.fasterxml.jackson.core.JsonGenerator;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.argument.StructuredArgument;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;

import java.io.IOException;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class TopicLogger implements StructuredArgument {
    @NonNull
    private final String topicName;
    private final Integer partition;

    static TopicLogger topic(ConsumerRecord<?, ?> record) {
        return new TopicLogger(record.topic(), record.partition());
    }

    static TopicLogger topic(ProducerRecord<?, ?> record) {
        return new TopicLogger(record.topic(), record.partition());
    }

    static TopicLogger topic(TopicPartition topicPartition) {
        return new TopicLogger(topicPartition.topic(), topicPartition.partition());
    }

    static TopicLogger topic(RecordMetadata metadata) {
        return new TopicLogger(metadata.topic(), metadata.partition());
    }

    @Override
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStringField("topic", topicName);
        if (partition != null) {
            generator.writeNumberField("partition", partition);
        }
    }

    @Override
    public String toString() {
        if (partition != null) {
            return String.format("%s (%s)", topicName, partition);
        }
        return String.format("%s", topicName);
    }
}
