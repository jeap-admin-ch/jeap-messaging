package ch.admin.bit.jeap.messaging.kafka.signature;

import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.MessageSignatureConfigurationException;
import ch.admin.bit.jeap.messaging.kafka.signature.publisher.SignaturePublisherProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.subscriber.SignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignaturePublisherCheckTest {

    @Mock
    private SignaturePublisherProperties signaturePublisherProperties;

    private SignaturePublisherCheck signaturePublisherCheck;

    @BeforeEach
    void setUp() {
        SignatureVerifier signatureVerifier = new SignatureVerifier();
        signaturePublisherCheck = new SignaturePublisherCheck(signaturePublisherProperties, signatureVerifier);
    }

    @Test
    void check_ok() {
        when(signaturePublisherProperties.signatureKey()).thenReturn(SigningTestHelper.MESSAGE_RECEIVER_PRIVATE_KEY.getBytes());
        when(signaturePublisherProperties.signatureCertificate()).thenReturn(SigningTestHelper.readBytesFromFile("classpath:signing/unittest/jme-messaging-receiverpublisher-service.crt"));
        signaturePublisherCheck.checkCertificateAndPrivateKeyConsistency();
    }

    @Test
    void check_nok() {
        when(signaturePublisherProperties.signatureKey()).thenReturn(SigningTestHelper.MESSAGE_RECEIVER_PRIVATE_KEY.getBytes());
        when(signaturePublisherProperties.signatureCertificate()).thenReturn(SigningTestHelper.readBytesFromFile("classpath:signing/unittest/jme-messaging-receiverpublisher-service2.crt"));

        assertThrows(MessageSignatureConfigurationException.class, () -> signaturePublisherCheck.checkCertificateAndPrivateKeyConsistency());
    }
}
