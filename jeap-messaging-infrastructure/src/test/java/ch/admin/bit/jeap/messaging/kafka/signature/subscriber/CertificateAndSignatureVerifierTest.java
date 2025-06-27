package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CertificateAndSignatureVerifierTest {

    private static final byte[] BYTES_TO_VALIDATE = {1, 2, 3, 4};
    private static final String SERVICE_NAME = "jme-messaging-receiverpublisher-service";
    private static final byte[] SIGNATURE = {1, 2};
    private static final String PRIVILEGED_PRODUCER = "mirrormaker";

    private SignatureCertificateWithChainValidity certificateWithChainValidity;
    private SignatureCertificateValidator certificateValidator;
    private SignatureVerifier signatureVerifier;
    private CertificateAndSignatureVerifier certificateAndSignatureVerifier;

    @BeforeEach
    void setUp() {
        certificateValidator = Mockito.mock(SignatureCertificateValidator.class);
        signatureVerifier = Mockito.mock(SignatureVerifier.class);
        certificateWithChainValidity = Mockito.mock(SignatureCertificateWithChainValidity.class);
        SignatureCertificate signatureCertificate = Mockito.mock(SignatureCertificate.class);
        when(certificateWithChainValidity.commonName()).thenReturn(SERVICE_NAME);
        when(certificateWithChainValidity.certificate()).thenReturn(signatureCertificate);

        SignatureSubscriberProperties props = new SignatureSubscriberProperties(true, null,
                Set.of(PRIVILEGED_PRODUCER), null, null);
        certificateAndSignatureVerifier = new CertificateAndSignatureVerifier(certificateValidator, signatureVerifier, props);
    }

    @Test
    void verifyValueSignatureWithService_returnTrue_whenValid() {
        when(signatureVerifier.verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE)).thenReturn(true);

        boolean verify = certificateAndSignatureVerifier.verifyValueSignature(SERVICE_NAME, BYTES_TO_VALIDATE, SIGNATURE, certificateWithChainValidity);
        assertTrue(verify);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verify(signatureVerifier).verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE);
    }

    @Test
    void verifyValueSignatureWithService_returnFalse_whenWrongSignature() {
        when(signatureVerifier.verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE)).thenReturn(false);

        boolean verify = certificateAndSignatureVerifier.verifyValueSignature(SERVICE_NAME, BYTES_TO_VALIDATE, SIGNATURE, certificateWithChainValidity);
        assertFalse(verify);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verify(signatureVerifier).verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE);
        ;
    }

    @Test
    void verifyValueSignatureWithService_doFail_whenCommonNameMismatch() {
        String otherServiceName = "my-other-service";

        assertThrows(CertificateValidationException.class, () -> certificateAndSignatureVerifier.verifyValueSignature(otherServiceName, BYTES_TO_VALIDATE, SIGNATURE, certificateWithChainValidity));
        verifyNoInteractions(signatureVerifier);
    }

    @Test
    void verifyValueSignatureWithService_doNotFail_whenCommonNameMismatch_ifPrivilegedProducer() {
        when(signatureVerifier.verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE)).thenReturn(true);
        when(certificateWithChainValidity.commonName()).thenReturn(PRIVILEGED_PRODUCER);

        boolean verify = certificateAndSignatureVerifier.verifyValueSignature(PRIVILEGED_PRODUCER, BYTES_TO_VALIDATE, SIGNATURE, certificateWithChainValidity);

        assertTrue(verify);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verify(signatureVerifier).verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE);
    }

    @Test
    void verifyKeySignatureWithoutService_returnTrue_whenValid() {
        when(signatureVerifier.verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE)).thenReturn(true);

        boolean verify = certificateAndSignatureVerifier.verifyKeySignature(BYTES_TO_VALIDATE, SIGNATURE, certificateWithChainValidity);
        assertTrue(verify);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verify(signatureVerifier).verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE);
    }

    @Test
    void verifyKeySignatureWithoutService_returnFalse_whenWrongSignature() {
        when(signatureVerifier.verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE)).thenReturn(false);

        boolean verify = certificateAndSignatureVerifier.verifyValueSignature(SERVICE_NAME, BYTES_TO_VALIDATE, SIGNATURE, certificateWithChainValidity);
        assertFalse(verify);
        verify(certificateValidator).validate(certificateWithChainValidity);
        verify(signatureVerifier).verify(certificateWithChainValidity.certificate(), BYTES_TO_VALIDATE, SIGNATURE);
        ;
    }
}
