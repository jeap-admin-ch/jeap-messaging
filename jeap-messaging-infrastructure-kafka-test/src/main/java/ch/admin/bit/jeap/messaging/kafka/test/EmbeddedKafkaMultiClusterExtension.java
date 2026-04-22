package ch.admin.bit.jeap.messaging.kafka.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

@Slf4j
public class EmbeddedKafkaMultiClusterExtension implements BeforeAllCallback, AfterAllCallback {

    private final EmbeddedKafkaBroker embeddedKafka1;
    private final EmbeddedKafkaBroker embeddedKafka2;
    private volatile boolean started = false;

    public EmbeddedKafkaMultiClusterExtension() {
        embeddedKafka1 = new EmbeddedKafkaKraftBroker(1, 1, "TOPIC");
        embeddedKafka2 = new EmbeddedKafkaKraftBroker(1, 1, "TOPIC");
    }

    public synchronized void startClusters() {
        if (started) {
            return;
        }
        log.info("Starting embedded kafka clusters...");
        embeddedKafka1.afterPropertiesSet();
        embeddedKafka2.afterPropertiesSet();
        started = true;
        log.info("Embedded kafka clusters started");
    }

    public String getBootstrapServers1() {
        startClusters();
        return embeddedKafka1.getBrokersAsString();
    }

    public String getBootstrapServers2() {
        startClusters();
        return embeddedKafka2.getBrokersAsString();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        if (!started) {
            return;
        }
        log.info("Stopping embedded kafka clusters...");
        embeddedKafka1.destroy();
        embeddedKafka2.destroy();
        log.info("Embedded kafka clusters stopped");
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        startClusters();
    }
}
