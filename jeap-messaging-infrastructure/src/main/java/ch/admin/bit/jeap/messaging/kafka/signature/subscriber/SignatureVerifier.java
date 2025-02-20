package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import ch.admin.bit.jeap.messaging.kafka.signature.common.CryptoProviderHelper;
import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.MessageSignatureValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

@Component
@RequiredArgsConstructor
public class SignatureVerifier {

    public boolean verify(SignatureCertificate certificate, byte[] bytesToValidate, byte[] signature) {
        try {
            return doCheckAuthenticity(certificate, bytesToValidate, signature);
        } catch (InvalidKeyException | SignatureException e) {
            throw MessageSignatureValidationException.signatureValidationFailed(e);
        }
    }

    private boolean doCheckAuthenticity(SignatureCertificate certificate, byte[] bytesToValidate, byte[] signatureBytes) throws InvalidKeyException, SignatureException {
        PublicKey publicKey = certificate.getPublicKey();
        Signature signature = CryptoProviderHelper.getSignatureInstance();
        signature.initVerify(publicKey);
        signature.update(bytesToValidate);

        return signature.verify(signatureBytes);
    }
}
