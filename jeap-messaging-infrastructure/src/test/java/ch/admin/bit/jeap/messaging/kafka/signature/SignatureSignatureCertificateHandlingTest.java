package ch.admin.bit.jeap.messaging.kafka.signature;

import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignatureSignatureCertificateHandlingTest {

    @Mock
    TaskScheduler taskScheduler;
    @Mock
    SignatureMetricsService signatureMetricsService;

    @Test
    void createCertificateHandling() {
        byte[] certificateBytes = SigningTestHelper.readBytesFromFile("classpath:signing/unittest/test.crt");
        SignatureCertificateHandling certificateHandling = SignatureCertificateHandling.create(certificateBytes, taskScheduler, signatureMetricsService, "test-jeap-service");
        assertNotNull(certificateHandling);
        assertNotNull(certificateHandling.getCertificateSerialNumber());
        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(Duration.of(1, HOURS)));
    }

    @Test
    void createCertificateHandling_fail_whenApplicationNameNotCorrespondingCN() {
        byte[] certificateBytes = SigningTestHelper.readBytesFromFile("classpath:signing/unittest/test.crt");
        assertThrows(CertificateException.class, () -> SignatureCertificateHandling.create(certificateBytes, taskScheduler, signatureMetricsService, "dummy"));
    }

    @Test
    void serialNumber() {
        SignatureCertificate certificate = mock(SignatureCertificate.class);
        byte[] serialNumber = {1, 2, 3, 4};
        when(certificate.getSerialNumber()).thenReturn(serialNumber);
        SignatureCertificateHandling certificateHandling = new SignatureCertificateHandling(certificate, taskScheduler, signatureMetricsService, "test");
        assertEquals(serialNumber, certificateHandling.getCertificateSerialNumber());
    }
}