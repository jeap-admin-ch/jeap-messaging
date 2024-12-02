package ch.admin.bit.jeap.messaging.kafka.auth.aws.iam;

import ch.admin.bit.jeap.messaging.kafka.auth.aws.iam.test.TestApp;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.msk.auth.iam.IAMClientCallbackHandler;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TestApp.class,
        properties = {
                "spring.application.name=testapp",
                "jeap.messaging.kafka.cluster.test-cluster.aws.msk.iam-auth-enabled=true",
                "jeap.messaging.kafka.cluster.test-cluster.aws.msk.region=eu-test-1"
        }
)
@SetSystemProperty(key = "javax.net.ssl.trustStore", value = "teststore")
@SetSystemProperty(key = "javax.net.ssl.trustStorePassword", value = "testsecret")
class AwsMskKafkaAuthPropertiesTest {

    @Autowired
    private AwsMskKafkaAuthProperties kafkaAuthProperties;

    @Test
    void authenticationProperties() {
        Map<String, Object> props = kafkaAuthProperties.authenticationProperties("test-cluster");

        assertThat(props)
                .containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_SSL.name())
                .containsEntry(SaslConfigs.SASL_MECHANISM, "AWS_MSK_IAM")
                .containsEntry(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "teststore")
                .containsEntry(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "testsecret")
                .containsEntry(SaslConfigs.SASL_JAAS_CONFIG, "software.amazon.msk.auth.iam.IAMLoginModule required;")
                .containsEntry(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS, IAMClientCallbackHandler.class.getName());
    }
}