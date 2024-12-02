package ch.admin.bit.jeap.messaging.kafka.auth;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.SslProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;

import java.util.HashMap;
import java.util.Map;

import static ch.admin.bit.jeap.messaging.kafka.properties.PropertyRequirements.requireNonNullValue;

@RequiredArgsConstructor
@Slf4j
public class KafkaSslAuthProperties implements KafkaAuthProperties {

    private final KafkaProperties kafkaProperties;

    @Override
    public Map<String, Object> authenticationProperties(String clusterName) {
        Map<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, requireNonNullValue("Security Protocol", kafkaProperties.getSecurityProtocol(clusterName)).name);

        // We take the as environment properties as default and might override them below
        KafkaTrustStoreUtility.setTrustStorePropertiesFromSystemProperties(props, false, false);
        // SSL with mutual authentication (mTLS).
        SslProperties sslProperties = kafkaProperties.getSslProperties(clusterName);
        validateSslProperties(sslProperties);

        setKeyStorePropertiesFromSslProperties(props, sslProperties);
        KafkaTrustStoreUtility.setTrustStorePropertiesFromSslProperties(props, sslProperties);

        return props;
    }

    private void validateSslProperties(SslProperties sslProperties) {
        requireNonNullValue("sslProperties", sslProperties);
        if (sslProperties.getKeyStoreLocation() == null || sslProperties.getKeyPassword() == null) {
            throw new IllegalStateException(
                    "When configuring kafka with ssl, the keyStoreLocation and the keyPassword must be set " +
                            "using jeap.kafka.messaging.cluster.<name>.keyStoreLocation and jeap.kafka.messaging.cluster.<name>.keyPassword.");
        }
    }

    private void setKeyStorePropertiesFromSslProperties(Map<String, Object> props, SslProperties sslProperties) {
        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, sslProperties.getKeyStoreLocation());
        props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, sslProperties.getKeyPassword());
        if (sslProperties.getKeyStoreType() != null) { // if empty the default SslConfigs.DEFAULT_SSL_KEYSTORE_TYPE will be taken
            props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, sslProperties.getKeyStoreType());
        }
    }
}