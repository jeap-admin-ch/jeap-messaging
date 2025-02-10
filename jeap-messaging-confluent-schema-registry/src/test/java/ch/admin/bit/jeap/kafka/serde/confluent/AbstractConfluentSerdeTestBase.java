package ch.admin.bit.jeap.kafka.serde.confluent;

import ch.admin.bit.jeap.kafka.serde.confluent.test.TestApp;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;


@SpringBootTest(
        classes = TestApp.class,
        properties = {
                "spring.application.name=testapp",
                "jeap.messaging.kafka.schemaRegistryUrl=mock://serdetest",
                "jeap.messaging.kafka.systemName=test",
                "jeap.messaging.kafka.serviceName=test-service",
                "jeap.messaging.kafka.expose-message-key-to-consumer=true"
        }
)
@EmbeddedKafka(controlledShutdown = true, partitions = 1)
public abstract class AbstractConfluentSerdeTestBase {

    @Autowired
    protected KafkaAvroSerdeProvider kafkaAvroSerdeProvider;

}