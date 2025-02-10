package ch.admin.bit.jeap.messaging.kafka.signature;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ByteSignerTest {

    @BeforeAll
    static void setup() {
        CryptoProviderHelper.installCryptoProvider();
    }

    @Test
    void sign() {
        byte[] keyBytes = SigningTestHelper.PRIVATE_KEY.getBytes();
        ByteSigner byteSigner = new ByteSigner(keyBytes);
        byte[] bytesToSign = {1, 2, 3, 4};

        byte[] signature = byteSigner.createSignature(bytesToSign);

        assertNotNull(signature);
    }

}