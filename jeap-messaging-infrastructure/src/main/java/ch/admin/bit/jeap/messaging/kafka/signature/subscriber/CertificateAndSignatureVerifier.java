package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
@Slf4j
public class CertificateAndSignatureVerifier {

    private final SignatureCertificateValidator certificateValidator;
    private final SignatureVerifier signatureVerifier;
    private final Set<String> privilegedProducerNames;

    public CertificateAndSignatureVerifier(SignatureCertificateValidator certificateValidator,
                                           SignatureVerifier signatureVerifier,
                                           SignatureSubscriberProperties signatureSubscriberProperties) {
        this.certificateValidator = certificateValidator;
        this.signatureVerifier = signatureVerifier;
        this.privilegedProducerNames = signatureSubscriberProperties.privilegedProducerCommonNames();
    }

    public boolean verifyKeySignature(byte[] bytesToValidate, byte[] signature, SignatureCertificateWithChainValidity cert) {
        return doVerify(bytesToValidate, signature, cert);
    }

    public boolean verifyValueSignature(String serviceName, byte[] bytesToValidate, byte[] signature, SignatureCertificateWithChainValidity cert) {
        validatePublisherIsPrivilegedOrNameMatchesCertCommonName(serviceName, cert);
        return doVerify(bytesToValidate, signature, cert);
    }

    /**
     * Checks the authenticity of the given bytes with the given signature and certificate serial number.
     *
     * @param serviceName the name of the service to check against the common name in the certificate, might be null
     */
    private boolean doVerify(byte[] bytesToValidate, byte[] signature, SignatureCertificateWithChainValidity cert) {
        certificateValidator.validate(cert);
        return signatureVerifier.verify(cert.certificate(), bytesToValidate, signature);
    }

    private void validatePublisherIsPrivilegedOrNameMatchesCertCommonName(String serviceName, SignatureCertificateWithChainValidity cert) {
        String commonName = cert.commonName();

        boolean certificateCommonNameMatchesPublisher = Objects.equals(serviceName, commonName);
        boolean certificateCommonNameIsPrivilegedProducer = isPrivilegedProducer(cert);

        if (!certificateCommonNameMatchesPublisher && !certificateCommonNameIsPrivilegedProducer) {
            log.error("Service name {} does not match CN of certificate {}", serviceName, commonName);
            throw CertificateValidationException.certificateCommonNameNotValid(serviceName, commonName);
        }
    }

    public boolean isPrivilegedProducer(SignatureCertificateWithChainValidity cert) {
        return privilegedProducerNames.contains(cert.commonName());
    }
}
