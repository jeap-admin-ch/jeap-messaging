package ch.admin.bit.jeap.messaging.kafka.signature;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureCertificateTest {

    private static final String SUBJECT_DISTINGUISHED_NAME = "CN=test-jeap-service";
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalDateTime YESTERDAY = NOW.minus(1, ChronoUnit.DAYS);
    private static final LocalDateTime TOMORROW = NOW.plus(1, ChronoUnit.DAYS);

    @Test
    void isExpired_returnFalse_whenNotExpired() {
        SignatureCertificate certificate = new SignatureCertificate(SUBJECT_DISTINGUISHED_NAME, null, YESTERDAY, TOMORROW);
        assertFalse(certificate.isExpired());
    }
    @Test
    void isExpired_returnTrue_whenExpired() {
        SignatureCertificate certificate = new SignatureCertificate(SUBJECT_DISTINGUISHED_NAME, null, NOW, NOW);
        assertTrue(certificate.isExpired());
    }

    @Test
    void isNotYetValid_returnFalse_whenAlreadyValid() {
        SignatureCertificate certificate = new SignatureCertificate(SUBJECT_DISTINGUISHED_NAME, null, YESTERDAY, TOMORROW);
        assertFalse(certificate.isNotYetValid());
    }

    @Test
    void isNotYetValid_returnTrue_whenNotYetValid() {
        SignatureCertificate certificate = new SignatureCertificate(SUBJECT_DISTINGUISHED_NAME, null, TOMORROW, TOMORROW);
        assertTrue(certificate.isNotYetValid());
    }

    @Test
    void getValidityRemainingDays_returnOne_whenValidUntilTomorrow() {
        SignatureCertificate certificate = new SignatureCertificate(SUBJECT_DISTINGUISHED_NAME, null, NOW, TOMORROW.plus(1, ChronoUnit.MINUTES));
        assertEquals(1, certificate.getValidityRemainingDays());
    }

    @Test
    void getValidityRemainingDays_returnMinusOne_whenValidUntilYesterday() {
        SignatureCertificate certificate = new SignatureCertificate(SUBJECT_DISTINGUISHED_NAME, null, YESTERDAY.minus(2, ChronoUnit.MINUTES), YESTERDAY.minus(1, ChronoUnit.MINUTES));
        assertEquals(-1, certificate.getValidityRemainingDays());
    }
}