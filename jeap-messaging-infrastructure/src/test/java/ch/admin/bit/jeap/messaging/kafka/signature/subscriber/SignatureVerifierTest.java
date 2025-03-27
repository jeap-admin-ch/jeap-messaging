package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import ch.admin.bit.jeap.messaging.kafka.signature.SigningTestHelper;
import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.MessageSignatureValidationException;
import ch.admin.bit.jeap.messaging.kafka.signature.publisher.ByteSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class SignatureVerifierTest {

    private final byte[] bytesToValidate = {1, 2, 3, 4};
    private byte[] signature;
    private SignatureCertificate certificate;

    private SignatureVerifier signatureVerifier = new SignatureVerifier();

    @BeforeEach
    void setUp() {
        ByteSigner byteSigner = new ByteSigner(SigningTestHelper.MESSAGE_RECEIVER_PRIVATE_KEY.getBytes());
        signature = byteSigner.createSignature(bytesToValidate);
        byte[] bytes = SigningTestHelper.readBytesFromFile("classpath:signing/unittest/jme-messaging-receiverpublisher-service.crt");
        certificate = SignatureCertificate.fromBytes(bytes);
    }

    @Test
    void validateSignature_returnValid_whenValid() {
        assertTrue(signatureVerifier.verify(certificate, bytesToValidate, signature));
    }

    @Test
    void validateSignature_returnInvalid_whenWrongSignature() {
        byte[] otherBytesToValidate = new byte[]{1, 2};
        assertFalse(signatureVerifier.verify(certificate, otherBytesToValidate, signature));
    }

    @Test
    void validateSignature_fail_whenValidationFailed() {
        PublicKey publicKey = null;
        SignatureCertificate certificate = Mockito.mock(SignatureCertificate.class);
        when(certificate.getPublicKey()).thenReturn(publicKey);

        assertThrows(MessageSignatureValidationException.class, () -> signatureVerifier.verify(certificate, bytesToValidate, signature));
    }
}
