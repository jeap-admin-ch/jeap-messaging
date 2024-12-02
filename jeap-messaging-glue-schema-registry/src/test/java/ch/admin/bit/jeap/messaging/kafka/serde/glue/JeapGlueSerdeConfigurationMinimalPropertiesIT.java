package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.test.TestApp;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = TestApp.class,
        properties = {
                "spring.application.name=testapp",
                "jeap.messaging.kafka.serviceName=test",
                "jeap.messaging.kafka.systemName=test",
                "jeap.messaging.kafka.cluster.default.aws.glue.registryName=testregistry",
                "jeap.messaging.kafka.cluster.default.aws.glue.region=eu-test-1"
        }
)
@Slf4j
class JeapGlueSerdeConfigurationMinimalPropertiesIT {

    @MockBean
    private AwsCredentialsProvider awsCredentialsProvider;

    @Autowired
    private Optional<KafkaAvroSerdeProvider> serdeProvider;

    // This test is expected to be successful with the minimal amount of necessary Glue config properties
    @Test
    void configure() {
        assertTrue(serdeProvider.isPresent());
    }
}