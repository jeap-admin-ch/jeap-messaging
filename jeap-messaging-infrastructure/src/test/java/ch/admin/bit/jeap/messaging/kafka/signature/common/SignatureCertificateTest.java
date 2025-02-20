package ch.admin.bit.jeap.messaging.kafka.signature.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class SignatureCertificateTest {

    private static final String SUBJECT_DISTINGUISHED_NAME = "CN=test-jeap-service";
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalDateTime YESTERDAY = NOW.minus(1, ChronoUnit.DAYS);
    private static final LocalDateTime TOMORROW = NOW.plus(1, ChronoUnit.DAYS);

    private X509Certificate certificate;

    @BeforeEach
    void setUp() {
        certificate = Mockito.mock(X509Certificate.class);
        X500Principal principal = Mockito.mock(X500Principal.class);;
        when(principal.getName()).thenReturn(SUBJECT_DISTINGUISHED_NAME);
        when(certificate.getIssuerX500Principal()).thenReturn(principal);
    }

    @Test
    void isExpired_returnFalse_whenNotExpired() {
        SignatureCertificate signatureCertificate = new SignatureCertificate(certificate, YESTERDAY, TOMORROW);
        assertFalse(signatureCertificate.isExpired());
    }
    @Test
    void isExpired_returnTrue_whenExpired() {
        SignatureCertificate signatureCertificate = new SignatureCertificate(certificate, NOW, NOW);
        assertTrue(signatureCertificate.isExpired());
    }

    @Test
    void isNotYetValid_returnFalse_whenAlreadyValid() {
        SignatureCertificate signatureCertificate = new SignatureCertificate(certificate, YESTERDAY, TOMORROW);
        assertFalse(signatureCertificate.isNotYetValid());
    }

    @Test
    void isNotYetValid_returnTrue_whenNotYetValid() {
        SignatureCertificate signatureCertificate = new SignatureCertificate(certificate, TOMORROW, TOMORROW);
        assertTrue(signatureCertificate.isNotYetValid());
    }

    @Test
    void getValidityRemainingDays_returnOne_whenValidUntilTomorrow() {
        SignatureCertificate signatureCertificate = new SignatureCertificate(certificate, NOW, TOMORROW.plus(1, ChronoUnit.MINUTES));
        assertEquals(1, signatureCertificate.getValidityRemainingDays());
    }

    @Test
    void getValidityRemainingDays_returnMinusOne_whenValidUntilYesterday() {
        SignatureCertificate signatureCertificate = new SignatureCertificate(certificate, YESTERDAY.minus(2, ChronoUnit.MINUTES), YESTERDAY.minus(1, ChronoUnit.MINUTES));
        assertEquals(-1, signatureCertificate.getValidityRemainingDays());
    }
}
