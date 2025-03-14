package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.encryption;

import ch.admin.bit.jeap.crypto.api.CryptoServiceProvider;
import ch.admin.bit.jeap.crypto.api.KeyId;
import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReference;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;

@Profile("key-id-crypto-service")
@Configuration
@JeapMessageProducerContract(value= JmeDeclarationCreatedEvent.TypeRef.class, encryptionKeyId = "testKey")
public class CryptoServiceTestConfig {

    @Bean
    public TestKeyReferenceCryptoService keyReferenceCryptoService() {
        return Mockito.spy(new TestKeyReferenceCryptoService());
    }

    @Bean
    public KeyIdCryptoService keyIdCryptoService() {
        return Mockito.spy(new TestKeyIdCryptoService(keyReferenceCryptoService(),
                Map.of(KeyId.of("testKey"), new KeyReference("test/key"))));
    }

    @Bean
    public CryptoServiceProvider cryptoServiceProvider(KeyIdCryptoService keyIdCryptoService) {
        return new CryptoServiceProvider(List.of(keyIdCryptoService));
    }

}
