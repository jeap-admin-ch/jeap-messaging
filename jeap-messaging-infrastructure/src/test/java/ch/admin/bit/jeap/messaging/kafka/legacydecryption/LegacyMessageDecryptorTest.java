package ch.admin.bit.jeap.messaging.kafka.legacydecryption;

import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

public class LegacyMessageDecryptorTest {

    private static final String PASSPHRASE = "testpw";

    @Test
    public void ok() throws GeneralSecurityException {
        String originalMessage = "testMessage";
        LegacyMessageDecryptor decryptor = new LegacyMessageDecryptor(PASSPHRASE);

        byte[] result = decryptor.decryptMessage(LegacyMessageEncryptor.encryptMessage(originalMessage.getBytes(), PASSPHRASE));

        assertEquals(originalMessage, new String(result));
    }

    @Test
    public void wrongPassphrase() {
        String originalMessage = "testMessage";
        LegacyMessageDecryptor decryptor = new LegacyMessageDecryptor("wrongPw");

        assertThrows(SerializationException.class, () ->
                decryptor.decryptMessage(LegacyMessageEncryptor.encryptMessage(originalMessage.getBytes(), PASSPHRASE)));
    }

    @Test
    public void notTransparent() throws GeneralSecurityException {
        String originalMessage = "testMessage";
        LegacyMessageDecryptor encryptor = new LegacyMessageDecryptor(PASSPHRASE);

        byte[] decrypted = LegacyMessageEncryptor.encryptMessage(originalMessage.getBytes(), PASSPHRASE);

        assertNotEquals(originalMessage.getBytes(), decrypted);
    }
}
