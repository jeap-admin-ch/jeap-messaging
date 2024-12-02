package ch.admin.bit.jeap.messaging.kafka.auth;

import ch.admin.bit.jeap.messaging.kafka.properties.cluster.SslProperties;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaTrustStoreUtilityTest {

    private static final String DEFAULT_STORE_LOCATION = "here/it/is/store.jks";
    private static final String DEFAULT_STORE_PASSWORD = "Swordfish";

    @Test
    void expectStoreLocationSetWhenPropertySetAndRequired() {
        setProperties(DEFAULT_STORE_LOCATION, DEFAULT_STORE_PASSWORD);
        Map<String, Object> props = new HashMap<>();

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSystemProperties(props, true, true);

        assertEquals(DEFAULT_STORE_LOCATION, props.get("ssl.truststore.location"));
    }

    @Test
    void expectStorePasswordSetWhenPropertySetAndRequired() {
        setProperties(DEFAULT_STORE_LOCATION, DEFAULT_STORE_PASSWORD);
        Map<String, Object> props = new HashMap<>();

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSystemProperties(props, true, true);

        assertEquals(DEFAULT_STORE_PASSWORD, props.get("ssl.truststore.password"));
    }

    @Test
    void expectStoreLocationSetWhenPropertySetAndNotRequired() {
        setProperties(DEFAULT_STORE_LOCATION, DEFAULT_STORE_PASSWORD);
        Map<String, Object> props = new HashMap<>();

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSystemProperties(props, false, false);

        assertEquals(DEFAULT_STORE_LOCATION, props.get("ssl.truststore.location"));
    }

    @Test
    void expectStorePasswordSetWhenPropertySetAndNotRequired() {
        setProperties(DEFAULT_STORE_LOCATION, DEFAULT_STORE_PASSWORD);
        Map<String, Object> props = new HashMap<>();

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSystemProperties(props, false, false);

        assertEquals(DEFAULT_STORE_PASSWORD, props.get("ssl.truststore.password"));
    }


    @Test
    void expectExceptionSetWhenStorePropertyNotSetButRequired() {
        setProperties(null, DEFAULT_STORE_PASSWORD);
        Map<String, Object> props = new HashMap<>();

        assertThrows(IllegalArgumentException.class, () -> KafkaTrustStoreUtility.setTrustStorePropertiesFromSystemProperties(props, true, true));
    }

    @Test
    void expectExceptionSetWhenPasswordPropertyNotSetButRequired() {
        setProperties(DEFAULT_STORE_LOCATION, null);
        Map<String, Object> props = new HashMap<>();

        assertThrows(IllegalArgumentException.class, () -> KafkaTrustStoreUtility.setTrustStorePropertiesFromSystemProperties(props, true, true));
    }

    @Test
    void expectStoreLocationIsNullWhenPropertyNotSetAndNotRequired() {
        setProperties(null, DEFAULT_STORE_PASSWORD);
        Map<String, Object> props = new HashMap<>();

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSystemProperties(props, false, true);

        assertNull(props.get("ssl.truststore.location"));
    }

    @Test
    void expectStorePasswordIsNullWhenPropertySetAndNotRequired() {
        setProperties(DEFAULT_STORE_LOCATION, null);
        Map<String, Object> props = new HashMap<>();

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSystemProperties(props, true, false);

        assertNull(props.get("ssl.truststore.password"));
    }

    @Test
    void expectTrustStoreLocationWhenSet() {
        SslProperties sslProperties = new SslProperties();
        sslProperties.setTrustStoreLocation(DEFAULT_STORE_LOCATION);
        Map<String, Object> props = new HashMap<>();

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSslProperties(props, sslProperties);
        assertEquals(DEFAULT_STORE_LOCATION, props.get("ssl.truststore.location"));
    }

    @Test
    void expectNoTrustStorePasswordWhenLocationSetAndPasswordNot() {
        SslProperties sslProperties = new SslProperties();
        sslProperties.setTrustStoreLocation(DEFAULT_STORE_LOCATION);
        Map<String, Object> props = new HashMap<>();
        props.put("ssl.truststore.password", DEFAULT_STORE_PASSWORD);

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSslProperties(props, sslProperties);
        assertNull(props.get("ssl.truststore.password"));
    }

    @Test
    void expectTrustStorePasswordWhenSetAndTrustStoreLocationSet() {
        SslProperties sslProperties = new SslProperties();
        sslProperties.setTrustStoreLocation(DEFAULT_STORE_LOCATION);
        sslProperties.setTrustStorePassword(DEFAULT_STORE_PASSWORD);
        Map<String, Object> props = new HashMap<>();

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSslProperties(props, sslProperties);
        assertEquals(DEFAULT_STORE_PASSWORD, props.get("ssl.truststore.password"));
    }

    @Test
    void expectTrustStorePasswordNullWhenSetAndTrustStoreLocationNotSet() {
        SslProperties sslProperties = new SslProperties();
        sslProperties.setTrustStorePassword(DEFAULT_STORE_PASSWORD);
        Map<String, Object> props = new HashMap<>();

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSslProperties(props, sslProperties);
        assertNull(props.get("ssl.truststore.password"));
    }

    @Test
    void expectTrustStoreTypeWhenSet() {
        SslProperties sslProperties = new SslProperties();
        sslProperties.setTrustStoreLocation(DEFAULT_STORE_LOCATION);
        sslProperties.setTrustStoreType("PEM");
        Map<String, Object> props = new HashMap<>();

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSslProperties(props, sslProperties);
        assertEquals("PEM", props.get("ssl.truststore.type"));
    }

    @Test
    void expectTrustStoreTypeNullWhenNotSetButSetBefore() {
        SslProperties sslProperties = new SslProperties();
        sslProperties.setTrustStoreLocation(DEFAULT_STORE_LOCATION);

        Map<String, Object> props = new HashMap<>();
        props.put("ssl.truststore.type", "PEM");

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSslProperties(props, sslProperties);
        assertNull(props.get("ssl.truststore.type"));
    }

    @Test
    void expectTrustStoreTypeNullWhenWhenSetAndTrustStoreLocationNotSet() {
        SslProperties sslProperties = new SslProperties();
        sslProperties.setTrustStoreType("PEM");

        Map<String, Object> props = new HashMap<>();

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSslProperties(props, sslProperties);
        assertNull(props.get("ssl.truststore.type"));
    }

    private void setProperties(String storeLocation, String storePassword) {
        setOrResetProperty("javax.net.ssl.trustStore", storeLocation);
        setOrResetProperty("javax.net.ssl.trustStorePassword", storePassword);
    }

    private void setOrResetProperty(String propertyName, String propertyValue) {
        if (propertyValue == null) {
            System.clearProperty(propertyName);
        } else {
            System.setProperty(propertyName, propertyValue);
        }
    }
}