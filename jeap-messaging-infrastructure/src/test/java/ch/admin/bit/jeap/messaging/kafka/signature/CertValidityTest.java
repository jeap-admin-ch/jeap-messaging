package ch.admin.bit.jeap.messaging.kafka.signature;

import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test class is used to test the validity of the certificate.
 * Please renew them if they are expired.
 * see {@link SigningTestHelper}
 */
public class CertValidityTest {

    @Test
    public void testRootCACertificateValidity() {
        checkValidity("rootCA.crt");
    }

    @Test
    public void testIntermediateCACertificateValidity() {
        checkValidity("intermediateCA.crt");
    }

    @Test
    public void testLeafCertificateValidity() {
        checkValidity("jme-messaging-receiverpublisher-service.crt");
    }

    @Test
    public void testLeaf2CertificateValidity() {
        checkValidity("jme-messaging-receiverpublisher-service2.crt");
    }

    private static void checkValidity(String certName) {
        byte[] bytes = SigningTestHelper.readBytesFromFile("classpath:signing/unittest/" + certName);
        SignatureCertificate certificate = SignatureCertificate.fromBytes(bytes);
        assertTrue(certificate.getValidityRemainingDays() > 2, "Please renew certificate " + certName + " it's valid for " + certificate.getValidityRemainingDays() + " days ");
    }
}
