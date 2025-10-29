package ch.admin.bit.jeap.messaging.kafka.signature.publisher;

import ch.admin.bit.jeap.messaging.kafka.signature.SignatureMetricsService;
import ch.admin.bit.jeap.messaging.kafka.signature.SigningTestHelper;
import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateException;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
        byte[] certificateBytes = SigningTestHelper.readBytesFromFile("classpath:signing/unittest/jme-messaging-receiverpublisher-service.crt");
        SignatureCertificateHandling certificateHandling = SignatureCertificateHandling.create(certificateBytes, taskScheduler, signatureMetricsService, "jme-messaging-receiverpublisher-service");
        assertNotNull(certificateHandling);
        assertNotNull(certificateHandling.getCertificateSerialNumber());
        verify(taskScheduler, times(2)).scheduleAtFixedRate(any(Runnable.class), eq(Duration.of(1, HOURS)));
    }

    @Test
    void createCertificateHandling_fail_whenApplicationNameNotCorrespondingCN() {
        byte[] certificateBytes = SigningTestHelper.readBytesFromFile("classpath:signing/unittest/jme-messaging-receiverpublisher-service.crt");
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

    @Test
    void checkCertificateValidity_NotExpired() {
        SignatureCertificate certificate = mock(SignatureCertificate.class);
        when(certificate.isExpired()).thenReturn(false);

        AtomicInteger calls = new AtomicInteger(0);
        TaskScheduler immediateTaskScheduler = new ImmediateTaskScheduler(calls);

        SignatureCertificateHandling signatureCertificateHandling = new SignatureCertificateHandling(certificate, immediateTaskScheduler, signatureMetricsService, "test");
        signatureCertificateHandling.init();

        assertEquals(2, calls.get());
    }

    @Test
    void checkCertificateValidity_Expired() {
        SignatureCertificate certificate = mock(SignatureCertificate.class);
        when(certificate.isExpired()).thenReturn(true);

        AtomicInteger calls = new AtomicInteger(0);
        TaskScheduler immediateTaskScheduler = new ImmediateTaskScheduler(calls);

        SignatureCertificateHandling signatureCertificateHandling = new SignatureCertificateHandling(certificate, immediateTaskScheduler, signatureMetricsService, "test");
        signatureCertificateHandling.init();

        assertEquals(2, calls.get());
    }

    @Test
    void checkCertificateValidity_YetValid() {
        SignatureCertificate certificate = mock(SignatureCertificate.class);
        when(certificate.isNotYetValid()).thenReturn(false);

        AtomicInteger calls = new AtomicInteger(0);
        TaskScheduler immediateTaskScheduler = new ImmediateTaskScheduler(calls);

        SignatureCertificateHandling signatureCertificateHandling = new SignatureCertificateHandling(certificate, immediateTaskScheduler, signatureMetricsService, "test");
        signatureCertificateHandling.init();

        assertEquals(2, calls.get());
    }

    @Test
    void checkCertificateValidity_NotYetValid() {
        SignatureCertificate certificate = mock(SignatureCertificate.class);
        when(certificate.isNotYetValid()).thenReturn(true);

        AtomicInteger calls = new AtomicInteger(0);
        TaskScheduler immediateTaskScheduler = new ImmediateTaskScheduler(calls);

        SignatureCertificateHandling signatureCertificateHandling = new SignatureCertificateHandling(certificate, immediateTaskScheduler, signatureMetricsService, "test");
        signatureCertificateHandling.init();

        assertEquals(2, calls.get());
    }


    @RequiredArgsConstructor
    private static class ImmediateTaskScheduler implements TaskScheduler {

        private final AtomicInteger calls;

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
            return null;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
            task.run();
            calls.incrementAndGet();
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
            return null;
        }

    }
}
