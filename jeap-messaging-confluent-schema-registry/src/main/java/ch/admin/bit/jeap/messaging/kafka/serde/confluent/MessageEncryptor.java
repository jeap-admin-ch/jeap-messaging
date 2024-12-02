package ch.admin.bit.jeap.messaging.kafka.serde.confluent;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.errors.SerializationException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * An Nifi compatible message encryptors using a symmetric key.
 */
@SuppressWarnings("WeakerAccess")
public class MessageEncryptor {
    private static final String OPENSSL_EVP_HEADER_MARKER = "Salted__";
    private static final int OPENSSL_EVP_HEADER_SIZE = 8;
    private static final int DEFAULT_SALT_LENGTH = 8;
    private static final int ITERATION_COUNT = 0;
    private static final String ALGORITHM = "PBEWITHMD5AND128BITAES-CBC-OPENSSL";
    private static final String PROVIDER = "BC";
    private final SecretKey secret;
    private final Cipher cipher;

    public MessageEncryptor(String passphrase) {
        Security.addProvider(new BouncyCastleProvider());
        try {
            final PBEKeySpec pbeKeySpec = new PBEKeySpec(passphrase.toCharArray());
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM, PROVIDER);
            secret = factory.generateSecret(pbeKeySpec);
            cipher = Cipher.getInstance(ALGORITHM, PROVIDER); //NOSONAR Nifi-compatible algorithm must be used
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException
                | NoSuchPaddingException e) {
            throw new ConfigException("failure to initialize Cipher for Nifi-compatible decryption");
        }
    }

    public byte[] encryptMessage(byte[] payload) {
        // generate salt
        byte[] salt = new byte[DEFAULT_SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        // initialize Cipher
        byte[] encrypted;
        try {
            final PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, ITERATION_COUNT);
            cipher.init(Cipher.ENCRYPT_MODE, secret, parameterSpec);
            encrypted = cipher.doFinal(payload);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException
                | BadPaddingException e) {
            throw new SerializationException("failed to decrypt payload", e);
        }

        // concatenate salt marker, salt and encrypted payload
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(OPENSSL_EVP_HEADER_MARKER.getBytes(StandardCharsets.US_ASCII));
            out.write(salt);
            out.write(encrypted);
        } catch (IOException e) {
            throw new SerializationException("failed to write salt and encrypted payload", e);
        }

        return out.toByteArray();
    }

    public byte[] decryptMessage(byte[] payload) {
        // check for salt marker
        byte[] header = Arrays.copyOf(payload, OPENSSL_EVP_HEADER_SIZE);
        if (!Arrays.equals(OPENSSL_EVP_HEADER_MARKER.getBytes(StandardCharsets.US_ASCII), header))
            throw new SerializationException("did not find salt marker for payload decryption");

        // read salt value
        int saltStartIndex = OPENSSL_EVP_HEADER_SIZE;
        int saltEndIndex = OPENSSL_EVP_HEADER_SIZE + DEFAULT_SALT_LENGTH;
        byte[] salt = Arrays.copyOfRange(payload, saltStartIndex, saltEndIndex);

        // initialize Cipher
        try {
            final PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, ITERATION_COUNT);
            cipher.init(Cipher.DECRYPT_MODE, secret, parameterSpec);
            return cipher.doFinal(Arrays.copyOfRange(payload, saltEndIndex, payload.length));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException
                | BadPaddingException e) {
            throw new SerializationException("failed to decrypt payload", e);
        }
    }
}
