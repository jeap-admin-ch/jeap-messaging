package ch.admin.bit.jeap.messaging.kafka.signature;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test class is used to test the validity of the certificate.
 * Please renew them if they are expired.
 * see {@link SigningTestHelper}
 */
public class CertValidityTest {

    @Test
    public void testCertificateValidity() {
        checkValidity("test.crt");
    }

    @Test
    public void testCACertificateValidity() {
        checkValidity("jeap-ca.crt");
    }

    private static void checkValidity(String certName) {
        byte[] bytes = SigningTestHelper.readBytesFromFile("classpath:signing/unittest/" + certName);
        SignatureCertificate certificate = SignatureCertificate.fromBytes(bytes);
        assertTrue(certificate.getValidityRemainingDays() > 2, "Please renew certificate " + certName + " it's valid for " + certificate.getValidityRemainingDays() + " days ");
    }
}