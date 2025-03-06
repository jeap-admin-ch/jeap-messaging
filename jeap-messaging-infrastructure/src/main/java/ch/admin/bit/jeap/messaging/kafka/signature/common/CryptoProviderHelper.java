package ch.admin.bit.jeap.messaging.kafka.signature.common;

import com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider;
import lombok.extern.slf4j.Slf4j;

import java.security.Signature;

@Slf4j
public class CryptoProviderHelper {

    private static final String CORRETTO_PROVIDER_NAME = AmazonCorrettoCryptoProvider.PROVIDER_NAME;
    private static final String SIGN_ALGORITHM = "SHA256withRSA";

    private static boolean correttoEnabled = false;

    public static void installCryptoProvider() {
        if (correttoEnabled) {
            return;
        }
        AmazonCorrettoCryptoProvider.install();
        try {
            AmazonCorrettoCryptoProvider.INSTANCE.assertHealthy();
            correttoEnabled = true;
        } catch (Throwable throwable) {
            log.warn("Corretto crypto provider is not enabled: " + throwable.getMessage());
            correttoEnabled = false;
        }
    }

    public static Signature getSignatureInstance() {
        try {
            return correttoEnabled ?
                    Signature.getInstance(SIGN_ALGORITHM, CORRETTO_PROVIDER_NAME) :
                    Signature.getInstance(SIGN_ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get signature instance", e);
        }
    }
}
