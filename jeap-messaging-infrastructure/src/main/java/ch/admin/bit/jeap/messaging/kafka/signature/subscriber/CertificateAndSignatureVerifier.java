package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import ch.admin.bit.jeap.messaging.kafka.signature.common.CertificateHelper;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CertificateAndSignatureVerifier {

    private final SubscriberCertificatesContainer subscriberCertificatesContainer;
    private final SignatureCertificateValidator certificateValidator;
    private final SignatureVerifier signatureVerifier;
    private final Set<String> privilegedProducerNames = Set.of("TODO-get-from-props-mirrormaker");

    public boolean verifyKeySignature(byte[] bytesToValidate, byte[] signature, byte[] certificateSerialNumber) {
        SignatureCertificateWithChainValidity cert = getCertificate(certificateSerialNumber);
        return doVerify(bytesToValidate, signature, cert);
    }

    public boolean verifyValueSignature(String serviceName, byte[] bytesToValidate, byte[] signature, byte[] certificateSerialNumber) {
        SignatureCertificateWithChainValidity cert = getCertificate(certificateSerialNumber);
        validatePublisherNameMatchesCertificateCommonName(serviceName, cert);
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

    private SignatureCertificateWithChainValidity getCertificate(byte[] certificateSerialNumber) {
        SignatureCertificateWithChainValidity certificateWithChainValidity = subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber);
        if (certificateWithChainValidity == null) {
            throw CertificateValidationException.certificateNotFound(certificateSerialNumber);
        }
        return certificateWithChainValidity;
    }

    private void validatePublisherNameMatchesCertificateCommonName(String serviceName, SignatureCertificateWithChainValidity certificateWithChainValidity) {
        String commonName = CertificateHelper.getCommonName(certificateWithChainValidity.getSubjectDistinguishedName());

        boolean certificateCommonNameMatchesPublisher = Objects.equals(serviceName, commonName);
        boolean certificateCommonNameIsPrivilegedProducer = privilegedProducerNames.contains(commonName);

        if (!certificateCommonNameMatchesPublisher && !certificateCommonNameIsPrivilegedProducer) {
            log.error("Service name {} does not match CN of certificate {}", serviceName, commonName);
            throw CertificateValidationException.certificateCommonNameNotValid(serviceName, commonName);
        }
    }
}
