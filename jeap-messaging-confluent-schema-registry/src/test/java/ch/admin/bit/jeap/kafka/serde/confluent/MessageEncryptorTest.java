package ch.admin.bit.jeap.kafka.serde.confluent;

import ch.admin.bit.jeap.messaging.kafka.serde.confluent.MessageEncryptor;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MessageEncryptorTest {
    @Test
    public void ok() {
        String originalMessage = "testMessage";
        MessageEncryptor encryptor = new MessageEncryptor("testpw");
        MessageEncryptor decryptor = new MessageEncryptor("testpw");

        byte[] result = decryptor.decryptMessage(encryptor.encryptMessage(originalMessage.getBytes()));

        assertEquals(originalMessage, new String(result));
    }


    @Test
    public void wrongPassphrase() {
        String originalMessage = "testMessage";
        MessageEncryptor encryptor = new MessageEncryptor("testpw");
        MessageEncryptor decryptor = new MessageEncryptor("wrongPw");

        assertThrows(SerializationException.class, () ->
                decryptor.decryptMessage(encryptor.encryptMessage(originalMessage.getBytes())));
    }

    @Test
    public void notTransparent() {
        String originalMessage = "testMessage";
        MessageEncryptor encryptor = new MessageEncryptor("testpw");

        byte[] decrypted = encryptor.encryptMessage(originalMessage.getBytes());

        assertNotEquals(originalMessage.getBytes(), decrypted);
    }
}
