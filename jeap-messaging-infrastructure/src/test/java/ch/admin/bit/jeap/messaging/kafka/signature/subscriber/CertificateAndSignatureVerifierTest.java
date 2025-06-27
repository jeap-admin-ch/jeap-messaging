package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void verifyValueSignatureWithService_returnTrue_whenValid() {
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(certificateWithChainValidity);
        when(signatureVerifier.verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE)).thenReturn(true);

        boolean verify = certificateAndSignatureVerifier.verifyValueSignature(SERVICE_NAME, BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER);
        assertTrue(verify);
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verify(signatureVerifier).verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE);
    }

    @Test
    void verifyValueSignatureWithService_returnFalse_whenWrongSignature() {
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(certificateWithChainValidity);
        when(signatureVerifier.verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE)).thenReturn(false);

        boolean verify = certificateAndSignatureVerifier.verifyValueSignature(SERVICE_NAME, BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER);
        assertFalse(verify);
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verify(signatureVerifier).verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE);;
    }

    @Test
    void verifyValueSignatureWithService_doFail_whenNoCertificateFound() {
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(null);

        assertThrows(CertificateValidationException.class, () -> certificateAndSignatureVerifier.verifyValueSignature(SERVICE_NAME, BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER));
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verifyNoInteractions(certificateValidator);
        verifyNoInteractions(signatureVerifier);
    }

    @Test
    void verifyValueSignatureWithService_doFail_whenCommonNameMismatch() {
        String otherServiceName = "my-other-service";
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(certificateWithChainValidity);

        assertThrows(CertificateValidationException.class, () -> certificateAndSignatureVerifier.verifyValueSignature(otherServiceName, BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER));
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verifyNoInteractions(signatureVerifier);
    }

    @Test
    void verifyKeySignatureWithoutService_returnTrue_whenValid() {
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(certificateWithChainValidity);
        when(signatureVerifier.verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE)).thenReturn(true);

        boolean verify = certificateAndSignatureVerifier.verifyKeySignature(BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER);
        assertTrue(verify);
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verify(signatureVerifier).verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE);
    }

    @Test
    void verifyKeySignatureWithoutService_returnFalse_whenWrongSignature() {
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(certificateWithChainValidity);
        when(signatureVerifier.verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE)).thenReturn(false);

        boolean verify = certificateAndSignatureVerifier.verifyValueSignature(SERVICE_NAME, BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER);
        assertFalse(verify);
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verify(signatureVerifier).verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE);;
    }

    @Test
    void verifyKeySignatureWithoutService_doFail_whenNoCertificateFound() {
        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER)).thenReturn(null);

        assertThrows(CertificateValidationException.class, () -> certificateAndSignatureVerifier.verifyValueSignature(SERVICE_NAME, BYTES_TO_VALIDATE, SIGNATURE, CERTIFICATE_SERIAL_NUMBER));
        verify(subscriberCertificatesContainer).getCertificateWithSerialNumber(CERTIFICATE_SERIAL_NUMBER);
        verifyNoInteractions(certificateValidator);
        verifyNoInteractions(signatureVerifier);
    }
}
