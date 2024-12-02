package ch.admin.bit.jeap.kafka.serde.confluent;

import ch.admin.bit.jeap.messaging.kafka.serde.confluent.CustomKafkaAvroDeserializer;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.CustomKafkaAvroSerializer;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.config.CustomKafkaAvroDeserializerConfig;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.config.CustomKafkaAvroSerializerConfig;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("resource")
class SerializedMessageReceiverIntegrationTest {
    private static final String TOPIC = "topic";
    private String bootstrapServer;

    @Test
    void checkEncryption() {
        startEmbeddedKafka();

        //Publish a message
        KafkaProducer<String, PaymentWithMessage> producer = new KafkaProducer<>(getProducerConfig());
        final String orderId = "id";
        final PaymentWithMessage payment = new PaymentWithMessage(orderId, "Ein", "Anderer", 100.00d);
        final ProducerRecord<String, PaymentWithMessage> record = new ProducerRecord<>(TOPIC,
                payment.getId().toString(), payment);
        producer.send(record);
        producer.flush();

        //Consume the message
        KafkaConsumer<String, PaymentWithMessage> nonEncryptedConsumer = new KafkaConsumer<>(getConsumerConfig());
        nonEncryptedConsumer.subscribe(Collections.singletonList(TOPIC));
        ConsumerRecords<String, PaymentWithMessage> records = nonEncryptedConsumer.poll(Duration.of(3, ChronoUnit.SECONDS));
        Iterator<ConsumerRecord<String, PaymentWithMessage>> iterator = records.iterator();
        ConsumerRecord<String, PaymentWithMessage> first = iterator.next();

        //Check that serialized message is there
        assertNotNull(first.value().getSerializedMessage());
        assertEquals(first.serializedValueSize(), first.value().getSerializedMessage().length);
    }

    private void startEmbeddedKafka() {
        EmbeddedKafkaBroker broker = new EmbeddedKafkaZKBroker(1, true, 1, TOPIC);
        broker.afterPropertiesSet();
        bootstrapServer = broker.getBrokersAsString();
    }

    private Properties getProducerConfig() {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CustomKafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://localhost:8081");
        props.put(CustomKafkaAvroSerializerConfig.ENCRYPT_MESSAGES_CONFIG, false);
        return props;
    }

    private Properties getConsumerConfig() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "encrypt-false");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CustomKafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://localhost:8081");
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put(CustomKafkaAvroDeserializerConfig.DECRYPT_MESSAGES_CONFIG, false);
        return props;
    }
}
