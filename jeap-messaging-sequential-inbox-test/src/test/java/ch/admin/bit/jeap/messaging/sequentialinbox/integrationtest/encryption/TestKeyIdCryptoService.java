package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.encryption;

import ch.admin.bit.jeap.crypto.api.*;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
class TestKeyIdCryptoService implements KeyIdCryptoService {

    private final KeyReferenceCryptoService keyReferenceCryptoService;
    private final Map<KeyId, KeyReference> keyIdKeyReferenceMap;
    
    @Override
    public byte[] encrypt(byte[] plaintext, KeyId keyId) {
        KeyReference keyReference = getKeyReferenceForKeyId(keyId).orElseThrow(
                () -> CryptoException.unknownKeyId(keyId));
        return keyReferenceCryptoService.encrypt(plaintext, keyReference);
    }

    @Override
    public byte[] decrypt(byte[] ciphertextCryptoContainer) {
        return keyReferenceCryptoService.decrypt(ciphertextCryptoContainer);
    }

    @Override
    public Set<KeyId> configuredKeyIds() {
        return keyIdKeyReferenceMap.keySet();
    }

    @Override
    public boolean canDecrypt(byte[] ciphertext) {
        return true;
    }

    private Optional<KeyReference> getKeyReferenceForKeyId(KeyId keyId) {
        return Optional.ofNullable(keyIdKeyReferenceMap.get(keyId));
    }
}
