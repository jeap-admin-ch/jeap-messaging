package ch.admin.bit.jeap.messaging.kafka.signature;

import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.MessageSignatureException;
import lombok.extern.slf4j.Slf4j;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

@Slf4j
public class ByteSigner {

    private static final String SIGN_ALGORITHM = "SHA256withRSA";

    private final PrivateKey privateKey;

    public ByteSigner(byte[] privateKeyBytes) {
        this(PrivateKeyFactory.createPrivateKey(privateKeyBytes));
    }

    ByteSigner(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public byte[] createSignature(byte[] bytes) {
        try {
            Signature signature = createSignature();
            signature.update(bytes);

            return signature.sign();
        } catch (NoSuchAlgorithmException | SignatureException | NoSuchProviderException | InvalidKeyException e) {
            log.error("Error while signing", e);
            throw MessageSignatureException.signatureCreationFailed(e);
        }
    }

    private Signature createSignature() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
        Signature signature = CryptoProviderHelper.correttoEnabled ?
                Signature.getInstance(SIGN_ALGORITHM, CryptoProviderHelper.CORRETTO_PROVIDER_NAME) :
                Signature.getInstance(SIGN_ALGORITHM);
        signature.initSign(privateKey);
        return signature;
    }

}
