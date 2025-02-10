package ch.admin.bit.jeap.messaging.kafka.signature;

import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.MessageSignatureConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(
        prefix = "jeap.messaging.authentication.publisher"
)
@Slf4j
public class SignatureProducerProperties {

    private final byte[] signatureKey;
    private final byte[] signatureCertificate;

    public SignatureProducerProperties(@Nullable String signatureKey, @Nullable String signatureCertificate) {
        this.signatureKey = signatureKey == null ? null : signatureKey.getBytes(StandardCharsets.UTF_8);
        this.signatureCertificate = signatureCertificate == null ? null : signatureCertificate.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isSigningEnabled() {
        return signatureKey != null && signatureCertificate != null;
    }

    public byte[] signatureKey() {
        return signatureKey;
    }

    public byte[] signatureCertificate() {
        return signatureCertificate;
    }

    void checkAndLogSigningDisabled() {
        if (signatureKey == null && signatureCertificate != null) {
            log.error("No signature key provided, but a certificate is provided. Please provide a key to sign messages");
            throw MessageSignatureConfigurationException.signatureConfigurationFailure("No signature key provided, but a certificate is provided. Please provide a key to sign messages");
        } else if (signatureCertificate == null && signatureKey != null) {
            log.error("No certificate provided, but a key is provided. Please provide a certificate to sign messages");
            throw MessageSignatureConfigurationException.signatureConfigurationFailure("No certificate provided, but a key is provided. Please provide a certificate to sign messages");
        } else {
            log.debug("Signing messages is disabled");
        }
    }
}