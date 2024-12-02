package ch.admin.bit.jeap.messaging.kafka.auth.aws.iam;

import ch.admin.bit.jeap.messaging.kafka.auth.aws.iam.test.TestApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TestApp.class,
        properties = {
                "spring.application.name=testapp",
                // jeap.messaging.kafka.aws.msk.iam-auth-enabled is not set
        }
)
class AwsMskKafkaAuthPropertiesIamAuthNotEnabledTest {

    @Autowired
    private Optional<AwsMskKafkaAuthProperties> kafkaAuthProperties;

    @Test
    void noBeanExpected() {
        assertThat(kafkaAuthProperties)
                .isEmpty();
    }
}