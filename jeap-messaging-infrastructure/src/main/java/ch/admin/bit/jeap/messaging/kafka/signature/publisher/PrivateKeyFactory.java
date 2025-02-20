package ch.admin.bit.jeap.messaging.kafka.signature.publisher;

import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.KeyException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Slf4j
@UtilityClass
public class PrivateKeyFactory {

    private static final String ALGORITHM = "RSA";

    static PrivateKey createPrivateKey(byte[] privateKeyBytes) {
        try {
            return doCreatePrivateKey(privateKeyBytes);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw KeyException.couldNotCreateKeyException(e);
        }
    }

    private static PrivateKey doCreatePrivateKey(byte[] privateKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            String pemContent = new String(privateKeyBytes)
                    .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(pemContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return createPrivateKey(keySpec);
        } catch (InvalidKeySpecException e) {
            // Backup for the case that the key is not in PEM format
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            return createPrivateKey(keySpec);
        }
    }

    private static PrivateKey createPrivateKey(KeySpec keySpec) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }
}
