package ch.admin.bit.jeap.messaging.kafka.signature.publisher;

import ch.admin.bit.jeap.messaging.kafka.signature.SignatureMetricsService;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureService;
import ch.admin.bit.jeap.messaging.kafka.signature.common.CryptoProviderHelper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;

import java.util.Optional;

@Slf4j
public class DefaultSignatureService implements SignatureService {

    private final SignaturePublisherProperties properties;
    private final TaskScheduler taskScheduler;
    private final SignatureMetricsService signatureMetricsService;
    private final String applicationName;
    private SignatureInjector signatureInjector;
    private SignatureCertificateHandling certificateHandling;

    public DefaultSignatureService(SignaturePublisherProperties properties, TaskScheduler taskScheduler,
                                   Optional<SignatureMetricsService> signatureMetricsService, @Value("${spring.application.name}") String applicationName) {
        this.properties = properties;
        this.taskScheduler = taskScheduler;
        this.signatureMetricsService = signatureMetricsService.orElse(null);
        this.applicationName = applicationName;
    }

    @PostConstruct
    public void init() {
        properties.checkAndLogSigningDisabled();
        if (properties.isSigningEnabled()) {
            CryptoProviderHelper.installCryptoProvider();
            certificateHandling = SignatureCertificateHandling.create(properties.signatureCertificate(), taskScheduler, signatureMetricsService, applicationName);
            signatureInjector = new SignatureInjector(new ByteSigner(properties.signatureKey()), certificateHandling.getCertificateSerialNumber());
        }
    }

    @Override
    public void injectSignature(Headers headers, byte[] bytesToSign, boolean isKey) {
        if (!properties.isSigningEnabled()) {
            return;
        }
        signatureInjector.injectSignature(headers, bytesToSign, isKey);
    }
}
