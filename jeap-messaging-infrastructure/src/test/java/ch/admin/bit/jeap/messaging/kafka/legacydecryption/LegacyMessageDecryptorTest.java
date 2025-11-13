package ch.admin.bit.jeap.messaging.kafka.legacydecryption;

import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

class LegacyMessageDecryptorTest {

    private static final String PASSPHRASE = "testpw";

    @Test
    void ok() throws GeneralSecurityException {
        String originalMessage = "testMessage";
        LegacyMessageDecryptor decryptor = new LegacyMessageDecryptor(PASSPHRASE);

        byte[] result = decryptor.decryptMessage(LegacyMessageEncryptor.encryptMessage(originalMessage.getBytes(), PASSPHRASE));

        assertEquals(originalMessage, new String(result));
    }

    @Test
    void wrongPassphrase() throws GeneralSecurityException {
        String originalMessage = "testMessage";
        LegacyMessageDecryptor decryptor = new LegacyMessageDecryptor("wrongPw");
        byte[] encryptedMessage = LegacyMessageEncryptor.encryptMessage(originalMessage.getBytes(), PASSPHRASE);

        assertThrows(SerializationException.class, () ->
                decryptor.decryptMessage(encryptedMessage));
    }

    @Test
    void notTransparent() throws GeneralSecurityException {
        String originalMessage = "testMessage";

        byte[] decrypted = LegacyMessageEncryptor.encryptMessage(originalMessage.getBytes(), PASSPHRASE);

        assertNotEquals(originalMessage.getBytes(), decrypted);
    }
}
