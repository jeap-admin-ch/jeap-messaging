package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test class is used to test the validity of the certificate.
 * Please renew them if they are expired.
 * see README.txt in jeap-messaging-infrastructure/src/test/resources/signing/unittest
 */
public class CertValidityTest {

    @Test
    public void testCertificateValidity() throws IOException {
        Object myConfig = loadConfig("application-test-signing-publisher.yml", "jeap.messaging.authentication.publisher.signature-certificate");

        byte[] bytes = myConfig.toString().getBytes();
        SignatureCertificate certificate = SignatureCertificate.fromBytes(bytes);
        assertTrue(certificate.getValidityRemainingDays() > 2, "Please renew certificate it's valid for " + certificate.getValidityRemainingDays() + " days ");
    }

    private Object loadConfig(String yamlFile, String configKey) throws IOException {
        Map<String, Object> yamlMap = loadYaml(yamlFile);
        Object config = getYamlValue(yamlMap, configKey);
        assertNotNull(config, "Config not found");
        return config;
    }

    private Object getYamlValue(Map<String, Object> yamlMap, String keyPath) {
        String[] keys = keyPath.split("\\."); // Split by dots
        Object value = yamlMap;

        for (String key : keys) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(key);
            } else {
                return null; // Key path does not exist
            }
        }
        return value;
    }

    private Map<String, Object> loadYaml(String yamlFile) throws IOException {
        Yaml yaml = new Yaml();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(yamlFile);
        assertNotNull(inputStream, "YAML file not found");
        return yaml.load(inputStream);
    }
}
