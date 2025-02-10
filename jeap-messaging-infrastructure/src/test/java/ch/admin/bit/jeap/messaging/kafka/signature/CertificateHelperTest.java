package ch.admin.bit.jeap.messaging.kafka.signature;

import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CertificateHelperTest {

    @Test
    void getCommonName() {
        assertEquals("Test-CN", CertificateHelper.getCommonName("CN=Test-CN,OU=Test,O=Test,L=Test,ST=Test,C=CH"));
    }

    @Test
    void getCommonName_returnNull_whenNoCN() {
        assertNull(CertificateHelper.getCommonName("OU=Test,O=Test,L=Test,ST=Test,C=CH"));
    }

    @Test
    void getCommonName_fail_whenNotValid() {
        assertThrows(CertificateException.class, () -> CertificateHelper.getCommonName("Some dummy text"));
    }
}