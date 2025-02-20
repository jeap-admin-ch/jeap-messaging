package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignatureCertificateValidator {

    public void validate(SignatureCertificateWithChainValidity signatureCertificate) {
        if (signatureCertificate.isExpired()) {
            throw CertificateValidationException.certificateExpired(signatureCertificate);
        }
        if (!signatureCertificate.isChainValid()) {
            throw CertificateValidationException.invalidChain(signatureCertificate);
        }
    }
}
