package ch.admin.bit.jeap.kafka.serde.confluent;

import ch.admin.bit.jeap.kafka.examples.Payment;
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
import org.apache.kafka.common.errors.SerializationException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("resource")
class EncryptionIntegrationTest {
    private static final String TOPIC = "topic";
    private String bootstrapServer;

    @Test
    void checkEncryption() {
        startEmbeddedKafka();

        //Publish a message
        KafkaProducer<String, Payment> producer = new KafkaProducer<>(getProducerConfig());
        final String orderId = "id";
        final Payment payment = new Payment(orderId, "Ein", "Anderer", 100.00d);
        final ProducerRecord<String, Payment> record = new ProducerRecord<>(TOPIC,
                payment.getId().toString(), payment);
        producer.send(record);
        producer.flush();

        //Read it without encryption, must not work
        assertThrows(SerializationException.class, () -> {
            KafkaConsumer<String, Payment> nonEncryptedPublisher = new KafkaConsumer<>(getConsumerConfig(false));
            nonEncryptedPublisher.subscribe(Collections.singletonList(TOPIC));
            nonEncryptedPublisher.poll(Duration.of(3, ChronoUnit.SECONDS));
        });

        //Read it with encryption
        KafkaConsumer<String, Payment> consumer = new KafkaConsumer<>(getConsumerConfig(true));
        consumer.subscribe(Collections.singletonList(TOPIC));
        ConsumerRecords<String, Payment> records = consumer.poll(Duration.of(3, ChronoUnit.SECONDS));

        Iterator<ConsumerRecord<String, Payment>> iterator = records.iterator();
        Payment first = iterator.next().value();
        assertEquals(100.00d, first.getValue(), 0.0);
        assertEquals("Ein", first.getVorname().toString());
        assertEquals("Anderer", first.getNachname().toString());
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
        props.put(CustomKafkaAvroSerializerConfig.ENCRYPT_MESSAGES_CONFIG, true);
        props.put(CustomKafkaAvroSerializerConfig.ENCRYPT_PASSPHRASE_CONFIG, "testPW");
        return props;
    }

    private Properties getConsumerConfig(boolean decrypt) {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "encrypt-" + decrypt);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CustomKafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://localhost:8081");
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put(CustomKafkaAvroDeserializerConfig.DECRYPT_MESSAGES_CONFIG, decrypt);
        props.put(CustomKafkaAvroDeserializerConfig.DECRYPT_PASSPHRASE_CONFIG, "testPW");
        return props;
    }
}
