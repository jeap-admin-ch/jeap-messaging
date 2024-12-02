package ch.admin.bit.jeap.messaging.kafka.auth;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;

import java.util.HashMap;
import java.util.Map;

import static ch.admin.bit.jeap.messaging.kafka.properties.PropertyRequirements.requireNonNullValue;

public class KafkaSaslAuthProperties implements KafkaAuthProperties {

    private final KafkaProperties kafkaProperties;

    public KafkaSaslAuthProperties(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public Map<String, Object> authenticationProperties(String clusterName) {
        Map<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, requireNonNullValue("Security Protocol", kafkaProperties.getSecurityProtocol(clusterName)).name);

        //If we want to use SASL_SSL we have to configure the system truststore
        if (SecurityProtocol.SASL_SSL.equals(kafkaProperties.getSecurityProtocol(clusterName))) {
            KafkaTrustStoreUtility.setTrustStorePropertiesFromSystemProperties(props, false, false);
            if (kafkaProperties.getSslProperties(clusterName) != null) { // if set we override the system properties
                KafkaTrustStoreUtility.setTrustStorePropertiesFromSslProperties(props, kafkaProperties.getSslProperties(clusterName));
            }
        }

        //If we want to use SASL for authentication we have to configure it
        if (SecurityProtocol.SASL_PLAINTEXT.equals(kafkaProperties.getSecurityProtocol(clusterName))
                || SecurityProtocol.SASL_SSL.equals(kafkaProperties.getSecurityProtocol(clusterName))) {
            props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
            props.put(SaslConfigs.SASL_JAAS_CONFIG, createSaslJaasConfig(clusterName));
        }
        return props;
    }

    private String createSaslJaasConfig(String clusterName) {
        return String.format("%s required username=\"%s\" password=\"%s\";",
                "org.apache.kafka.common.security.scram.ScramLoginModule",
                requireNonNullValue("SASL username", kafkaProperties.getUsername(clusterName)),
                requireNonNullValue("SASL password", kafkaProperties.getPassword(clusterName))
        );
    }
}
