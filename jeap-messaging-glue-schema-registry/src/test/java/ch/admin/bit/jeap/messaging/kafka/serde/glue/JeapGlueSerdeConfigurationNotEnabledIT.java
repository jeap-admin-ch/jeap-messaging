package ch.admin.bit.jeap.messaging.kafka.serde.glue;

import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import ch.admin.bit.jeap.messaging.kafka.serde.glue.test.TestApp;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = TestApp.class,
        properties = {
                "spring.application.name=testapp"
                // jeap.messaging.kafka.aws.glue.registryName is not set - Glue is not enabled
        }
)
@Slf4j
class JeapGlueSerdeConfigurationNotEnabledIT {

    @Autowired
    private Optional<KafkaAvroSerdeProvider> serdeProvider;

    // This test is expected to be successful without any Glue config properties as Glue is not enabled
    @Test
    void configure() {
        assertTrue(serdeProvider.isEmpty());
    }
}
