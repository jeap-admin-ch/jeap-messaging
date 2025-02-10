package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.messaging.kafka.auth.KafkaAuthProperties;
import ch.admin.bit.jeap.messaging.kafka.auth.KafkaSslAuthProperties;
import ch.admin.bit.jeap.messaging.kafka.test.integration.test.TestApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TestApp.class,
        properties = {
                "spring.application.name=jme-messaging-receiverpublisher-service",
                "jeap.messaging.kafka.systemName=test-system",
                "jeap.messaging.kafka.serviceName=test-service",
                "jeap.messaging.kafka.use-schema-registry=false",
                "jeap.messaging.kafka.auto-register-schema=false",
                "jeap.messaging.kafka.cluster.default.security-protocol=SSL",
                "jeap.messaging.kafka.cluster.default.ssl.key-store-location=keystore.pem",
                "jeap.messaging.kafka.cluster.default.ssl.key-password=dummy",
                "jeap.messaging.kafka.cluster.default.ssl.key-store-type=PEM",
                "jeap.messaging.kafka.cluster.default.ssl.trust-store-location=truststore.pem",
                "jeap.messaging.kafka.cluster.default.ssl.trust-store-type=PEM",
                "jeap.messaging.kafka.cluster.default.bootstrap-servers=localhost:1234"
        }
)
class SslConfigurationIT {

    @Autowired
    KafkaAuthProperties kafkaAuthProperties;
    @MockitoBean
    KeyIdCryptoService keyIdCryptoService;

    @Test
    void whenSslThenKafkaSslAuthProperties() {
        assertThat(kafkaAuthProperties)
                .isInstanceOf(KafkaSslAuthProperties.class);
    }
}
