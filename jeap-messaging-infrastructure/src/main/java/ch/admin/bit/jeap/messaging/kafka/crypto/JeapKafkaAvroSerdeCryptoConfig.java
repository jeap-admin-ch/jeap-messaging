package ch.admin.bit.jeap.messaging.kafka.crypto;

import ch.admin.bit.jeap.crypto.api.CryptoServiceProvider;
import ch.admin.bit.jeap.crypto.api.KeyId;
import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class JeapKafkaAvroSerdeCryptoConfig {

    public static final String ENCRYPTED_VALUE_HEADER_NAME = "jeap_encrypted_value";
    public static final byte[] ENCRYPTED_VALUE_HEADER_TRUE = new byte[]{1};

    private final CryptoServiceProvider cryptoServiceProvider;

    // Which message types must be encrypted and what's the key id to be used
    private final Map<String, KeyId> messageTypeNameKeyIdMap;

    public KeyIdCryptoService getKeyIdCryptoService(KeyId keyId) {
        return cryptoServiceProvider.getKeyIdCryptoService(keyId);
    }

    public JeapKafkaAvroSerdeCryptoConfig(CryptoServiceProvider cryptoServiceProvider, Map<String, KeyId> messageTypeNameKeyIdMap) {
        Objects.requireNonNull(messageTypeNameKeyIdMap, "A message type name to key id map has to be provided.");
        this.cryptoServiceProvider = cryptoServiceProvider;
        this.messageTypeNameKeyIdMap = new HashMap<>(messageTypeNameKeyIdMap);
    }

    public Optional<KeyId> getKeyIdForMessageTypeName(String messageTypeName) {
        return Optional.ofNullable(messageTypeNameKeyIdMap.get(messageTypeName));
    }

    public KeyIdCryptoService getKeyIdCryptoServiceForDecryption(byte[] originalBytes) {
        return cryptoServiceProvider.getKeyIdCryptoServiceForDecryption(originalBytes);
    }
}
