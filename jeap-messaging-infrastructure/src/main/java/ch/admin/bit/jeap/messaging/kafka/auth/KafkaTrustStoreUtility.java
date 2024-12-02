package ch.admin.bit.jeap.messaging.kafka.auth;

import ch.admin.bit.jeap.messaging.kafka.properties.cluster.SslProperties;
import org.apache.kafka.common.config.SslConfigs;

import java.util.Map;

import static ch.admin.bit.jeap.messaging.kafka.properties.PropertyRequirements.requireNonNullValue;

public final class KafkaTrustStoreUtility {

    private static final String TRUST_STORE_PROPERTY_NAME = "javax.net.ssl.trustStore";
    private static final String TRUST_STORE_PASSWORD_PROPERTY_NAME = "javax.net.ssl.trustStorePassword";

    public static void setTrustStorePropertiesFromSslProperties(Map<String, Object> props, SslProperties sslProperties) {
        if (sslProperties.getTrustStoreLocation() != null) {
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, sslProperties.getTrustStoreLocation());
            if (sslProperties.getTrustStorePassword() != null) {
                props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, sslProperties.getTrustStorePassword());
            } else {
                props.remove(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG); // unset values that maybe have been set before, this is necessary in order to avoid problems.
            }
            if (sslProperties.getTrustStoreType() != null) { // if empty the default SslConfigs.DEFAULT_SSL_KEYSTORE_TYPE will be taken
                props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, sslProperties.getTrustStoreType());
            } else {
                props.remove(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG); // unset values that maybe have been set before, this is necessary in order to avoid problems.
            }
        }
    }

    public static void setTrustStorePropertiesFromSystemProperties(Map<String, Object> props, boolean requiredLocation, boolean requiredPassword) {
        setTrustStorePropertyFromSystemProperty(props, requiredLocation, TRUST_STORE_PROPERTY_NAME, SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG);
        setTrustStorePropertyFromSystemProperty(props, requiredPassword, TRUST_STORE_PASSWORD_PROPERTY_NAME, SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG);
    }

    private static void setTrustStorePropertyFromSystemProperty(Map<String, Object> props, boolean required, String systemPropertyName, String targetPropertyName) {
        String value = System.getProperty(systemPropertyName);
        if (required) {
            requireNonNullValue(systemPropertyName, value);
        }
        if (value != null) {
            props.put(targetPropertyName, value);
        }
    }
}
