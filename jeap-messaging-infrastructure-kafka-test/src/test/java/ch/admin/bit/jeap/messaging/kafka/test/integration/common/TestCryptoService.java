package ch.admin.bit.jeap.messaging.kafka.test.integration.common;

import ch.admin.bit.jeap.crypto.api.CryptoService;

public class TestCryptoService implements CryptoService {

    static private final TestKeyReferenceCryptoService testKeyReferenceCryptoService = new TestKeyReferenceCryptoService();

    @Override
    public byte[] encrypt(byte[] plaintext) {
        return testKeyReferenceCryptoService.encrypt(plaintext, null);
    }

    @Override
    public byte[] decrypt(byte[] ciphertextCryptoContainer) {
        return testKeyReferenceCryptoService.decrypt(ciphertextCryptoContainer);
    }

}
