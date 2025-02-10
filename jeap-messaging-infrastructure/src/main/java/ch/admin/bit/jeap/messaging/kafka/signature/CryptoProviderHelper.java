package ch.admin.bit.jeap.messaging.kafka.signature;

import com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CryptoProviderHelper {

    static final String CORRETTO_PROVIDER_NAME = AmazonCorrettoCryptoProvider.PROVIDER_NAME;

    static boolean correttoEnabled = false;

    static void installCryptoProvider() {
        AmazonCorrettoCryptoProvider.install();
        try {
            AmazonCorrettoCryptoProvider.INSTANCE.assertHealthy();
            correttoEnabled = true;
        } catch (Throwable throwable) {
            log.warn("Corretto cypto provider is not enabled");
            correttoEnabled = false;
        }
    }
}
