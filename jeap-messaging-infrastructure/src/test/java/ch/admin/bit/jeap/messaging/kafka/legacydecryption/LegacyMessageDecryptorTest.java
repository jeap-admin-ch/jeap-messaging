package ch.admin.bit.jeap.messaging.kafka.legacydecryption;

import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LegacyMessageDecryptorTest {

    @Test
    public void ok() {
        String originalMessage = "testMessage";
        LegacyMessageDecryptor encryptor = new LegacyMessageDecryptor("testpw");
        LegacyMessageDecryptor decryptor = new LegacyMessageDecryptor("testpw");

        byte[] result = decryptor.decryptMessage(encryptor.encryptMessage(originalMessage.getBytes()));

        assertEquals(originalMessage, new String(result));
    }

    @Test
    public void wrongPassphrase() {
        String originalMessage = "testMessage";
        LegacyMessageDecryptor encryptor = new LegacyMessageDecryptor("testpw");
        LegacyMessageDecryptor decryptor = new LegacyMessageDecryptor("wrongPw");

        assertThrows(SerializationException.class, () ->
                decryptor.decryptMessage(encryptor.encryptMessage(originalMessage.getBytes())));
    }

    @Test
    public void notTransparent() {
        String originalMessage = "testMessage";
        LegacyMessageDecryptor encryptor = new LegacyMessageDecryptor("testpw");

        byte[] decrypted = encryptor.encryptMessage(originalMessage.getBytes());

        assertNotEquals(originalMessage.getBytes(), decrypted);
    }
}
