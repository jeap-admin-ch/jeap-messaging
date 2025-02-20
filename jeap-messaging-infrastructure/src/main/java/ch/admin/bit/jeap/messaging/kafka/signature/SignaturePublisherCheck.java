package ch.admin.bit.jeap.messaging.kafka.signature;

import ch.admin.bit.jeap.messaging.kafka.signature.common.CryptoProviderHelper;
import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.MessageSignatureConfigurationException;
import ch.admin.bit.jeap.messaging.kafka.signature.publisher.ByteSigner;
import ch.admin.bit.jeap.messaging.kafka.signature.publisher.SignaturePublisherProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.subscriber.SignatureVerifier;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class SignaturePublisherCheck {

    private static final byte[] BYTES_TO_SIGN = {1, 2, 3, 4, 5, 6, 7, 8, 9};

    private final SignaturePublisherProperties signaturePublisherProperties;
    private final SignatureVerifier signatureVerifier;

    @PostConstruct
    void checkCertificateAndPrivateKeyConsistency() {
        CryptoProviderHelper.installCryptoProvider();

        ByteSigner byteSigner = new ByteSigner(signaturePublisherProperties.signatureKey());
        byte[] signature = byteSigner.createSignature(BYTES_TO_SIGN);

        SignatureCertificate certificate = SignatureCertificate.fromBytes(signaturePublisherProperties.signatureCertificate());

        boolean verify = signatureVerifier.verify(certificate, BYTES_TO_SIGN, signature);
        if (!verify) {
            String message = "The private key provided for signing kafka messages does not match the public key in the certificate for " + certificate.getSubjectDistinguishedName();
            log.error(message);
            throw MessageSignatureConfigurationException.signatureConfigurationFailure(message);
        }
    }

}
