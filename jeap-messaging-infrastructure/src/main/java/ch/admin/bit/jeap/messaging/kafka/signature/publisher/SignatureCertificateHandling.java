package ch.admin.bit.jeap.messaging.kafka.signature.publisher;

import ch.admin.bit.jeap.messaging.kafka.signature.SignatureMetricsService;
import ch.admin.bit.jeap.messaging.kafka.signature.common.CertificateHelper;
import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
@Slf4j
class SignatureCertificateHandling {

    private static final Duration TASK_INTERVAL = Duration.ofHours(1);

    private final SignatureCertificate certificate;
    private final TaskScheduler taskScheduler;
    private final SignatureMetricsService signatureMetricsService;
    private final String applicationName;
    private AtomicLong validityDaysRemaining;

    public byte[] getCertificateSerialNumber() {
        return certificate.getSerialNumber();
    }

    private void initialCheck() {
        checkCommonName();
        checkCertificateValidity();
    }

    private void checkCommonName() {
        if (!Objects.equals(applicationName, CertificateHelper.getCommonName(certificate.getSubjectDistinguishedName()))) {
            log.error("Application name {} does not match CN of certificate {}", applicationName, certificate.getSubjectDistinguishedName());
            throw CertificateException.certificateCnNotValid(applicationName, certificate.getSubjectDistinguishedName());
        }
    }

    private void checkCertificateValidity() {
        if (certificate.isExpired()) {
            log.warn("Signing Certificate is expired, please renew");
        }
        if (certificate.isNotYetValid()) {
            log.warn("Signing Certificate is not yet valid");
        }
    }

    private void init() {
        initMetrics();
        initTaskScheduler();
    }

    private void initMetrics() {
        if (signatureMetricsService != null) {
            validityDaysRemaining = new AtomicLong(certificate.getValidityRemainingDays());
            signatureMetricsService.initCertificateValidityRemainingDays(() -> validityDaysRemaining.get());
        }
    }

    private void initTaskScheduler() {
        taskScheduler.scheduleAtFixedRate(() -> {
            checkCertificateValidity();
            setMetrics();
        }, TASK_INTERVAL);
    }

    private void setMetrics() {
        validityDaysRemaining.set(certificate.getValidityRemainingDays());
    }

    static SignatureCertificateHandling create(byte[] certificateBytes, TaskScheduler taskScheduler, @Nullable SignatureMetricsService signatureMetricsService, String applicationName) {
        SignatureCertificate signatureCertificate = SignatureCertificate.fromBytes(certificateBytes);
        SignatureCertificateHandling certificateHandling = new SignatureCertificateHandling(signatureCertificate, taskScheduler, signatureMetricsService, applicationName);
        certificateHandling.initialCheck();
        certificateHandling.init();
        return certificateHandling;
    }
}
