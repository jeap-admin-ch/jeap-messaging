package ch.admin.bit.jeap.kafka.serde.confluent.test;

import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.SerdeUtils;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.CustomKafkaAvroDeserializer;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.config.KafkaConfluentAvroSerdeProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureAuthenticityService;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaPropertyFactory;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Key and Value deserializers are initialized as part of KafkaConsumer creation, therefore they're not exposed as
 * beans. This configuration class creates and exposes both as close to runtime as possible.
 */
@Configuration
public class DeserializationConfig {

    @Bean
    protected Deserializer<Object> valueDeserializer(Environment environment) {
        KafkaProperties kafkaProperties = JeapKafkaPropertyFactory.createJeapKafkaProperties(environment);
        KafkaConfluentAvroSerdeProperties serdeProperties = createKafkaConfluentAvroSerdeProperties(kafkaProperties, null, null);
        KafkaAvroDeserializer customKafkaAvroValueDeserializer = new CustomKafkaAvroDeserializer();
        return SerdeUtils.deserializerWithErrorHandling("default", customKafkaAvroValueDeserializer, false, serdeProperties);
    }

    @Bean
    protected Deserializer<Object> keyDeserializer(Environment environment) {
        KafkaProperties kafkaProperties = JeapKafkaPropertyFactory.createJeapKafkaProperties(environment);
        KafkaConfluentAvroSerdeProperties serdeProperties = createKafkaConfluentAvroSerdeProperties(kafkaProperties, null, null);
        return SerdeUtils.createKeyDeserializer("default", CustomKafkaAvroDeserializer::new, kafkaProperties, serdeProperties);
    }

    private KafkaConfluentAvroSerdeProperties createKafkaConfluentAvroSerdeProperties(KafkaProperties kafkaProperties, JeapKafkaAvroSerdeCryptoConfig cryptoConfig, SignatureAuthenticityService signatureAuthenticityService) {
        return new KafkaConfluentAvroSerdeProperties(kafkaProperties, cryptoConfig, signatureAuthenticityService);
    }

}
