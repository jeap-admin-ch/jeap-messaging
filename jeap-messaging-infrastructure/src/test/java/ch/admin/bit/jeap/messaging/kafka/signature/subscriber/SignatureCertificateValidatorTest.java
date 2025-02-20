package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SignatureCertificateValidatorTest {

    private SignatureCertificateValidator signatureCertificateValidator = new SignatureCertificateValidator();

    @Test
    void validate_doNotFail_whenValid() {
        SignatureCertificateWithChainValidity certificate = mock(SignatureCertificateWithChainValidity.class);
        when(certificate.isExpired()).thenReturn(false);
        when(certificate.isChainValid()).thenReturn(true);

        signatureCertificateValidator.validate(certificate);
    }

    @Test
    void validate_doFail_whenExpired() {
        SignatureCertificateWithChainValidity certificate = mock(SignatureCertificateWithChainValidity.class);
        when(certificate.isExpired()).thenReturn(true);
        when(certificate.isChainValid()).thenReturn(true);

        assertThrows(CertificateValidationException.class, () -> signatureCertificateValidator.validate(certificate));
    }

    @Test
    void validate_doFail_whenChainInvalid() {
        SignatureCertificateWithChainValidity certificate = mock(SignatureCertificateWithChainValidity.class);
        when(certificate.isExpired()).thenReturn(false);
        when(certificate.isChainValid()).thenReturn(false);

        assertThrows(CertificateValidationException.class, () -> signatureCertificateValidator.validate(certificate));
    }
}
