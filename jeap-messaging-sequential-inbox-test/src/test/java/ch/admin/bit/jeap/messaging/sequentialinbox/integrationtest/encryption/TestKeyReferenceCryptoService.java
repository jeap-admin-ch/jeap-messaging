package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.encryption;

import ch.admin.bit.jeap.crypto.api.KeyReference;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestKeyReferenceCryptoService implements KeyReferenceCryptoService {

    @Override
    public byte[] encrypt(byte[] plaintext, KeyReference wrappingKeyReference) {
        log.debug("Encrypting.");
        return encryptDecrypt(plaintext);

    }

    @Override
    public byte[] decrypt(byte[] ciphertextCryptoContainer) {
        log.debug("Decrypting.");
        return encryptDecrypt(ciphertextCryptoContainer);
    }

    @Override
    public boolean canDecrypt(byte[] ciphertext) {
        return true;
    }

    private static byte[] encryptDecrypt(byte[] value) {
        final byte b42 = 42;
        byte[] xor42 = new byte[value.length];
        int i = 0;
        for (byte b : value) {
            xor42[i++] = (byte) (b ^ b42);
        }
        return xor42;
    }

}
