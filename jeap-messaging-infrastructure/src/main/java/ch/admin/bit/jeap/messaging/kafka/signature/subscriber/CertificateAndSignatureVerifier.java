package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import ch.admin.bit.jeap.messaging.kafka.signature.common.CertificateHelper;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class CertificateAndSignatureVerifier {

    private final SubscriberCertificatesContainer subscriberCertificatesContainer;
    private final SignatureCertificateValidator certificateValidator;
    private final SignatureVerifier signatureVerifier;

    public boolean verify(byte[] bytesToValidate, byte[] signature, byte[] certificateSerialNumber) {
        return doVerify(null, bytesToValidate, signature, certificateSerialNumber);
    }

    public boolean verify(String serviceName, byte[] bytesToValidate, byte[] signature, byte[] certificateSerialNumber) {
        return doVerify(serviceName, bytesToValidate, signature, certificateSerialNumber);
    }

    /**
     * Checks the authenticity of the given bytes with the given signature and certificate serial number.
     *
     * @param serviceName the name of the service to check against the common name in the certificate, might be null
     */
    private boolean doVerify(String serviceName, byte[] bytesToValidate, byte[] signature, byte[] certificateSerialNumber) {
        SignatureCertificateWithChainValidity certificateWithChainValidity = subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber);
        if (certificateWithChainValidity == null) {
            throw CertificateValidationException.certificateNotFound(certificateSerialNumber);
        }
        certificateValidator.validate(certificateWithChainValidity);

        String commonName = CertificateHelper.getCommonName(certificateWithChainValidity.getSubjectDistinguishedName());
        if (serviceName != null && !Objects.equals(serviceName, commonName)) {
            log.error("Service name {} does not match CN of certificate {}", serviceName, commonName);
            throw CertificateValidationException.certificateCommonNameNotValid(serviceName, commonName);
        }
        return signatureVerifier.verify(certificateWithChainValidity.certificate(), bytesToValidate, signature);
    }

}
