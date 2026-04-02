package ch.admin.bit.jeap.messaging.kafka.health;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.kafka.core.KafkaAdmin;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KafkaClusterHealthIndicator implements HealthIndicator, DisposableBean {

    // Extra buffer on top of the Kafka-side DescribeClusterOptions timeout so that Kafka's own
    // timeout fires first, giving a meaningful broker error rather than a bare Future TimeoutException.
    private static final long FUTURE_TIMEOUT_BUFFER_MS = 1000;

    private final KafkaProperties kafkaProperties;
    private final Duration responseTimeout;
    private final Duration downAfter;
    private final BeanFactory beanFactory;
    private final JeapKafkaBeanNames beanNames;
    private final ConcurrentHashMap<String, Instant> firstFailurePerCluster = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AdminClient> adminClientPerCluster = new ConcurrentHashMap<>();

    public KafkaClusterHealthIndicator(KafkaProperties kafkaProperties,
                                       BeanFactory beanFactory,
                                       Duration responseTimeout,
                                       Duration downAfter) {
        this.kafkaProperties = kafkaProperties;
        this.responseTimeout = responseTimeout;
        this.downAfter = downAfter;
        this.beanFactory = beanFactory;
        this.beanNames = new JeapKafkaBeanNames(kafkaProperties.getDefaultClusterName());
        log.info("Initialized KafkaClusterHealthIndicator with clusters: {}", kafkaProperties.clusterNames());
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        for (String clusterName : kafkaProperties.clusterNames()) {
            details.put(clusterName, checkCluster(clusterName));
        }
        boolean allUp = details.values().stream()
                .map(h -> (Health) h)
                .allMatch(h -> Status.UP.equals(h.getStatus()));
        Health.Builder builder = allUp ? Health.up() : Health.down();
        details.forEach(builder::withDetail);
        return builder.build();
    }

    private Health checkCluster(String clusterName) {
        String beanName = beanNames.getAdminBeanName(clusterName);
        int timeoutMs = (int) responseTimeout.toMillis();
        try {
            AdminClient adminClient = adminClientPerCluster.computeIfAbsent(clusterName, name -> {
                KafkaAdmin kafkaAdmin = beanFactory.getBean(beanName, KafkaAdmin.class);
                return AdminClient.create(kafkaAdmin.getConfigurationProperties());
            });
            DescribeClusterOptions options = new DescribeClusterOptions().timeoutMs(timeoutMs);
            DescribeClusterResult result = adminClient.describeCluster(options);
            String clusterId = result.clusterId().get(timeoutMs + FUTURE_TIMEOUT_BUFFER_MS, TimeUnit.MILLISECONDS);
            int nodeCount = result.nodes().get(timeoutMs + FUTURE_TIMEOUT_BUFFER_MS, TimeUnit.MILLISECONDS).size();
            firstFailurePerCluster.remove(clusterName);
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodeCount)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return handleFailure(clusterName, e);
        } catch (Exception e) {
            return handleFailure(clusterName, e);
        }
    }

    @Override
    public void destroy() {
        adminClientPerCluster.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("Error closing cached AdminClient", e);
            }
        });
    }

    private Health handleFailure(String clusterName, Exception e) {
        Instant now = Instant.now();
        Instant firstFailure = firstFailurePerCluster.computeIfAbsent(clusterName, _ -> now);
        Duration failingFor = Duration.between(firstFailure, now);
        if (failingFor.compareTo(downAfter) < 0) {
            log.warn("Kafka health check for cluster '{}' failed, reporting UP during recovery grace period ({}/{})",
                    clusterName, failingFor, downAfter, e);
            return Health.up()
                    .withDetail("recovering", true)
                    .withDetail("failingSince", firstFailure.toString())
                    .build();
        }
        log.warn("Kafka health check for cluster '{}' failed for {}, reporting DOWN", clusterName, failingFor, e);
        return Health.down(e)
                .withDetail("failingSince", firstFailure.toString())
                .build();
    }
}
