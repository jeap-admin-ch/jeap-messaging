package ch.admin.bit.jeap.messaging.kafka.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;

import java.util.Map;

@Slf4j
public class EmbeddedKafkaMultiClusterExtension implements BeforeAllCallback, AfterAllCallback {
    public static final int BASE_PORT = 12365; // random number

    private final EmbeddedKafkaBroker embeddedKafka1;
    private final EmbeddedKafkaBroker embeddedKafka2;

    private static int brokerIdCounter = 100;

    public EmbeddedKafkaMultiClusterExtension(int portOffset) {
        this(BASE_PORT, portOffset);
    }

    public EmbeddedKafkaMultiClusterExtension(int basePort, int portOffset) {
        embeddedKafka1 = new EmbeddedKafkaZKBroker(1, true, 1, "TOPIC");
        setNextBrokerId(embeddedKafka1);
        embeddedKafka1.kafkaPorts(basePort + portOffset);

        embeddedKafka2 = new EmbeddedKafkaZKBroker(1, true, 1, "TOPIC");
        setNextBrokerId(embeddedKafka2);
        embeddedKafka2.kafkaPorts(basePort + 1 + portOffset);
    }

    private static void setNextBrokerId(EmbeddedKafkaBroker embeddedKafka) {
        embeddedKafka.brokerProperties(Map.of(
                "broker.id.generation.enable", "false",
                "broker.id", String.valueOf(brokerIdCounter++)));
    }

    public static EmbeddedKafkaMultiClusterExtension withBasePortAndOffset(int basePort, int portOffset) {
        return new EmbeddedKafkaMultiClusterExtension(portOffset);
    }

    public static EmbeddedKafkaMultiClusterExtension withPortOffset(int portOffset) {
        return new EmbeddedKafkaMultiClusterExtension(portOffset);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        log.info("Stopping embedded kafka clusters...");
        embeddedKafka1.destroy();
        embeddedKafka2.destroy();
        log.info("Embedded kafka clusters stopped");
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        log.info("Starting embedded kafka clusters...");
        embeddedKafka1.afterPropertiesSet();
        embeddedKafka2.afterPropertiesSet();
        log.info("Embedded kafka clusters started");
    }
}
