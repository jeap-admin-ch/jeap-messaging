package ch.admin.bit.jeap.messaging.kafka.crypto;

import ch.admin.bit.jeap.crypto.api.CryptoServiceProvider;
import ch.admin.bit.jeap.crypto.api.KeyId;
import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsProvider;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Configuration
public class KafkaCryptoConfiguration {

    private static final String MESSAGE_CONTRACT_ROLE_PRODUCER = "producer";

    private final KafkaProperties kafkaProperties;
    private final Environment environment;
    private final ContractsProvider contractsProvider;
    private final Optional<KeyIdCryptoService> keyIdCryptoService;

    @PostConstruct
    public void assertValidConfiguration() {
        boolean isAbnOrProd = Arrays.stream(environment.getActiveProfiles()).anyMatch(profile ->
                profile.equalsIgnoreCase("abn") || profile.equalsIgnoreCase("prod"));
        if (isAbnOrProd && kafkaProperties.isMessageTypeEncryptionDisabled()) {
            throw new IllegalStateException("Disabling encryption for message types via 'jeap.messaging.kafka.message-type-encryption-disabled' " +
                                            "is prohibited on 'abn' and 'prod' environments.");
        }
        Map<String, KeyId> messageTypeNameEncryptionKeyIdMap = getMessageTypeNameEncryptionKeyIdMap();
        if (!messageTypeNameEncryptionKeyIdMap.isEmpty() && keyIdCryptoService.isEmpty()) {
            throw new IllegalStateException("Some message types are configured to be encrypted, but no key id crypto service " +
                                            "has been provided: " + String.join(", ", messageTypeNameEncryptionKeyIdMap.keySet()));
        }
    }

    @ConditionalOnBean(CryptoServiceProvider.class)
    @Bean
    public JeapKafkaAvroSerdeCryptoConfig getJeapKafkaAvroSerdeCryptoConfig(CryptoServiceProvider cryptoServiceProvider) {
        Map<String, KeyId> messageTypeNameEncryptionKeyIdMap = getMessageTypeNameEncryptionKeyIdMap();
        Set<String> unknownKeyIdStrings = messageTypeNameEncryptionKeyIdMap.values().stream()
                .filter(keyId -> !cryptoServiceProvider.configuredKeyIds().contains(keyId))
                .map(KeyId::id)
                .collect(Collectors.toSet());
        if (!unknownKeyIdStrings.isEmpty()) {
            throw new IllegalStateException("Some key ids declared in the application's publisher message contracts are not " +
                    "known to any of the configured key id crypto service instances: " + String.join(", ", unknownKeyIdStrings));
        }
        return new JeapKafkaAvroSerdeCryptoConfig(cryptoServiceProvider, messageTypeNameEncryptionKeyIdMap);
    }

    private Map<String, KeyId> getMessageTypeNameEncryptionKeyIdMap() {
        if (kafkaProperties.isMessageTypeEncryptionDisabled()) {
            return Map.of();
        }
        return contractsProvider.getContracts().stream().
                filter(c -> MESSAGE_CONTRACT_ROLE_PRODUCER.equals(c.getRole())).
                filter(c -> c.getEncryptionKeyId() != null).
                collect(Collectors.toMap(Contract::getMessageTypeName, c -> KeyId.of(c.getEncryptionKeyId())));
    }
}
