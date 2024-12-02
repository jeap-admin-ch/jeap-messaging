package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.messaging.kafka.auth.KafkaAuthProperties;
import ch.admin.bit.jeap.messaging.kafka.auth.aws.iam.AwsMskKafkaAuthProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.JeapGlueAvroDeserializer;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.JeapGlueAvroSerializer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.test.TestApp;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TestApp.class,
        properties = {
                "spring.application.name=jme-messaging-receiverpublisher-service",
                "jeap.messaging.kafka.systemName=test-system",
                "jeap.messaging.kafka.serviceName=test-service",
                "jeap.messaging.kafka.cluster.default.aws.glue.registryName=testregistry",
                "jeap.messaging.kafka.cluster.default.aws.glue.region=eu-test-1",
                "jeap.messaging.kafka.cluster.default.aws.msk.iam-auth-enabled=true",
                "jeap.messaging.kafka.cluster.default.aws.msk.region=eu-test-1",
                "jeap.messaging.kafka.cluster.default.bootstrap-servers=localhost:1234"
        }
)
@SetSystemProperty(key = "javax.net.ssl.trustStore", value = "teststore")
@SetSystemProperty(key = "javax.net.ssl.trustStorePassword", value = "testsecret")
class AwsConfigurationIT {

    @Autowired
    KafkaAuthProperties kafkaAuthProperties;
    @Autowired
    KafkaAvroSerdeProvider kafkaAvroSerdeProvider;
    @MockBean
    KeyIdCryptoService keyIdCryptoService;

    @Test
    void awsMskIamAuthAndGlueConfigurationTakesPrecedence() {
        assertThat(kafkaAuthProperties)
                .isInstanceOf(AwsMskKafkaAuthProperties.class);
        assertThat(kafkaAvroSerdeProvider.getValueSerializer())
                .isInstanceOf(JeapGlueAvroSerializer.class);
        assertThat(kafkaAvroSerdeProvider.getGenericDataRecordDeserializer())
                .isInstanceOf(JeapGlueAvroDeserializer.class);
    }
}
