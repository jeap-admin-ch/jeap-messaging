package ch.admin.bit.jeap.messaging.kafka.serde;

import ch.admin.bit.jeap.crypto.api.KeyId;
import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.CreateSerializedMessageHolder;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import lombok.experimental.UtilityClass;
import org.apache.avro.generic.GenericContainer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.Optional;
import java.util.function.Supplier;

@UtilityClass
public class SerdeUtils {

    public static Deserializer<Object> createKeyDeserializer(String clusterName,
                                                             Supplier<Deserializer<Object>> deserializerProvider,
                                                             KafkaProperties kafkaProperties,
                                                             KafkaAvroSerdeProperties serdeProperties) {
        if (kafkaProperties.isExposeMessageKeyToConsumer()) {
            Deserializer<Object> jeapDeserializer = deserializerProvider.get();
            jeapDeserializer.configure(serdeProperties.avroDeserializerProperties(clusterName), true);
            return SerdeUtils.deserializerWithErrorHandling(clusterName, jeapDeserializer, true, serdeProperties);
        } else {
            // Don't make message keys available to consumers by default in order to prevent misusing Avro keys as data containers
            return new EmptyKeyDeserializer();
        }
    }

    public static Deserializer<Object> deserializerWithErrorHandling(String clusterName,
                                                                     Deserializer<Object> deserializer, boolean isKey,
                                                                     KafkaAvroSerdeProperties serdeProperties) {
        ErrorHandlingDeserializer<Object> errorHandlingValueDeserializer =
                new ErrorHandlingDeserializer<>(deserializer);
        errorHandlingValueDeserializer.setFailedDeserializationFunction(new CreateSerializedMessageHolder());
        errorHandlingValueDeserializer.configure(serdeProperties.avroDeserializerProperties(clusterName), isKey);
        return errorHandlingValueDeserializer;
    }

    public static boolean isMessageEncryptedWithJeapCrypto(boolean isKey, Headers headers) {
        return !isKey &&
               headers != null &&
               headers.headers(JeapKafkaAvroSerdeCryptoConfig.ENCRYPTED_VALUE_HEADER_NAME).iterator().hasNext();
    }

    public static byte[] decryptIfEncrypted(boolean isKey, JeapKafkaAvroSerdeCryptoConfig cryptoConfig,
                                            String topic, byte[] originalBytes, Headers headers) {
        boolean messageEncryptedWithJeapCrypto = SerdeUtils.isMessageEncryptedWithJeapCrypto(isKey, headers);
        if (messageEncryptedWithJeapCrypto) {
            return decryptWithJeapCrypto(cryptoConfig, topic, originalBytes);
        } else {
            return originalBytes;
        }
    }

    public static byte[] decryptWithJeapCrypto(JeapKafkaAvroSerdeCryptoConfig cryptoConfig, String topic, byte[] originalBytes) {
        if (cryptoConfig != null) {
            return cryptoConfig.getKeyIdCryptoServiceForDecryption(originalBytes).decrypt(originalBytes);
        } else {
            throw new IllegalStateException("The headers of a message on topic '" + topic + "' indicate that the received " +
                                            " message is encrypted but no key id crypto service has been configured for decrypting the message.");
        }
    }

    public static byte[] encryptPayloadIfRequired(boolean isKey, JeapKafkaAvroSerdeCryptoConfig cryptoConfig,
                                                  Headers headers, Object record, byte[] payload) {
        if (!isKey && (cryptoConfig != null)) {
            String messageTypeName = requireMessageTypeName(record);
            Optional<KeyId> keyIdOptional = cryptoConfig.getKeyIdForMessageTypeName(messageTypeName);
            if (keyIdOptional.isPresent()) {
                KeyId keyId = keyIdOptional.get();
                byte[] encryptedPayload = cryptoConfig.getKeyIdCryptoService(keyId).encrypt(payload, keyId);
                headers.add(
                        JeapKafkaAvroSerdeCryptoConfig.ENCRYPTED_VALUE_HEADER_NAME,
                        JeapKafkaAvroSerdeCryptoConfig.ENCRYPTED_VALUE_HEADER_TRUE);
                return encryptedPayload;
            }
        }

        return payload;
    }

    private static String requireMessageTypeName(Object record) {
        if (record instanceof GenericContainer gc) {
            return gc.getSchema().getName();
        }
        throw new IllegalStateException("The record object is not an instance of GenericContainer - unable to determine Avro schema name.");
    }
}
