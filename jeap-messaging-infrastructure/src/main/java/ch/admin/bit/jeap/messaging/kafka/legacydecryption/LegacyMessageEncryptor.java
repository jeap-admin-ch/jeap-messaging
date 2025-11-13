package ch.admin.bit.jeap.messaging.kafka.legacydecryption;

import lombok.SneakyThrows;
import org.apache.kafka.common.errors.SerializationException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;

public class LegacyMessageEncryptor {

    private static final Cipher CIPHER = createCipher();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static byte[] encryptMessage(byte[] payload, String passphrase) throws GeneralSecurityException {
        // generate salt
        byte[] salt = new byte[LegacyMessageDecryptor.DEFAULT_SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);

        // initialize Cipher
        byte[] encrypted;
        try {
            final PBEKeySpec pbeKeySpec = new PBEKeySpec(passphrase.toCharArray());
            SecretKeyFactory factory = SecretKeyFactory.getInstance(LegacyMessageDecryptor.ALGORITHM, LegacyMessageDecryptor.PROVIDER);
            SecretKey secret = factory.generateSecret(pbeKeySpec);
            final PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, LegacyMessageDecryptor.ITERATION_COUNT);
            CIPHER.init(Cipher.ENCRYPT_MODE, secret, parameterSpec);
            encrypted = CIPHER.doFinal(payload);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException
                 | BadPaddingException e) {
            throw new SerializationException("failed to decrypt payload", e);
        }

        // concatenate salt marker, salt and encrypted payload
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(LegacyMessageDecryptor.OPENSSL_EVP_HEADER_MARKER_BYTES);
            out.write(salt);
            out.write(encrypted);
        } catch (IOException e) {
            throw new SerializationException("failed to write salt and encrypted payload", e);
        }

        return out.toByteArray();
    }

    @SneakyThrows
    private static Cipher createCipher() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        return Cipher.getInstance(LegacyMessageDecryptor.ALGORITHM, BouncyCastleProvider.PROVIDER_NAME); //NOSONAR Nifi-compatible algorithm must be used
    }
}
