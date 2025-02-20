package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CertificateAndSignatureVerifierTest {

    private static final byte[] BYTES_TO_VALIDATE = {1, 2, 3, 4};
    private static final String SERVICE_NAME = "jme-messaging-receiverpublisher-service";
    private static final byte[] SIGNATURE = {1, 2};
    private static final byte[] CERTIFICATE_SERIAL_NUMBER = {4, 3, 2, 1};

    private SignatureCertificateWithChainValidity certificateWithChainValidity;

    private SubscriberCertificatesContainer subscriberCertificatesContainer;
    private SignatureCertificateValidator certificateValidator;
    private SignatureVerifier signatureVerifier;

    private CertificateAndSignatureVerifier certificateAndSignatureVerifier;


    @BeforeEach
    void setUp() {
        subscriberCertificatesContainer = Mockito.mock(SubscriberCertificatesContainer.class);
        certificateValidator = Mockito.mock(SignatureCertificateValidator.class);
        signatureVerifier = Mockito.mock(SignatureVerifier.class);
        certificateWithChainValidity = Mockito.mock(SignatureCertificateWithChainValidity.class);
        SignatureCertificate signatureCertificate = Mockito.mock(SignatureCertificate.class);
        when(certificateWithChainValidity.getSubjectDistinguishedName()).thenReturn("CN=" + SERVICE_NAME);
        when(certificateWithChainValidity.certificate()).thenReturn(signatureCertificate);

        certificateAndSignatureVerifier = new CertificateAndSignatureVerifier(subscriberCertificatesContainer, certificateValidator, signatureVerifier);
    }

    @Test
    void verifyWithService_returnTrue_whenValid() {
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(certificateWithChainValidity);
        when(signatureVerifier.verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE)).thenReturn(true);

        boolean verify = certificateAndSignatureVerifier.verify(SERVICE_NAME, BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER);
        assertTrue(verify);
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verify(signatureVerifier).verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE);
    }

    @Test
    void verifyWithService_returnFalse_whenWrongSignature() {
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(certificateWithChainValidity);
        when(signatureVerifier.verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE)).thenReturn(false);

        boolean verify = certificateAndSignatureVerifier.verify(SERVICE_NAME, BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER);
        assertFalse(verify);
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verify(signatureVerifier).verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE);;
    }

    @Test
    void verifyWithService_doFail_whenNoCertificateFound() {
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(null);

        assertThrows(CertificateValidationException.class, () -> certificateAndSignatureVerifier.verify(SERVICE_NAME, BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER));
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verifyNoInteractions(certificateValidator);
        verifyNoInteractions(signatureVerifier);
    }

    @Test
    void verifyWithService_doFail_whenCommonNameMismatch() {
        String otherServiceName = "my-other-service";
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(certificateWithChainValidity);

        assertThrows(CertificateValidationException.class, () -> certificateAndSignatureVerifier.verify(otherServiceName, BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER));
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verifyNoInteractions(signatureVerifier);
    }

    @Test
    void verifyWithoutService_returnTrue_whenValid() {
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(certificateWithChainValidity);
        when(signatureVerifier.verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE)).thenReturn(true);

        boolean verify = certificateAndSignatureVerifier.verify(BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER);
        assertTrue(verify);
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verify(signatureVerifier).verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE);
    }

    @Test
    void verifyWithoutService_returnFalse_whenWrongSignature() {
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(certificateWithChainValidity);
        when(signatureVerifier.verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE)).thenReturn(false);

        boolean verify = certificateAndSignatureVerifier.verify(SERVICE_NAME, BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER);
        assertFalse(verify);
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verify(signatureVerifier).verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE);;
    }

    @Test
    void verifyWithoutService_doFail_whenNoCertificateFound() {
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(null);

        assertThrows(CertificateValidationException.class, () -> certificateAndSignatureVerifier.verify(SERVICE_NAME, BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER));
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verifyNoInteractions(certificateValidator);
        verifyNoInteractions(signatureVerifier);
    }
}
