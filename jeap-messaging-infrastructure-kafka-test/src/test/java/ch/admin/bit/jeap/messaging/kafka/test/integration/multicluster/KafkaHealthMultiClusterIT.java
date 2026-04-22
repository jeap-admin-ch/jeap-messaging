package ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster;

import ch.admin.bit.jeap.messaging.kafka.test.EmbeddedKafkaMultiClusterExtension;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.application.name=jme-messaging-receiverpublisher-service",
        "management.endpoints.web.exposure.include=health",
        "management.endpoint.health.show-details=always",
        "jeap.messaging.kafka.embedded=false",
        "jeap.messaging.kafka.systemName=test",
        "jeap.messaging.kafka.errorTopicName=errorTopic",
        "jeap.messaging.kafka.message-type-encryption-disabled=true",
        "jeap.messaging.kafka.cluster.default.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.default.schemaRegistryUrl=mock://health-multi-cluster-registry-1",
        "jeap.messaging.kafka.cluster.default.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.default.schemaRegistryPassword=unused",
        "jeap.messaging.kafka.cluster.aws.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.aws.schemaRegistryUrl=mock://health-multi-cluster-registry-2",
        "jeap.messaging.kafka.cluster.aws.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.aws.schemaRegistryPassword=unused"
})
@DirtiesContext
class KafkaHealthMultiClusterIT {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static final int PORT_OFFSET = 30;

    @RegisterExtension
    static EmbeddedKafkaMultiClusterExtension embeddedKafka =
            new EmbeddedKafkaMultiClusterExtension();

    @DynamicPropertySource
    static void registerBootstrapServers(DynamicPropertyRegistry registry) {
        registry.add("jeap.messaging.kafka.cluster.default.bootstrapServers", embeddedKafka::getBootstrapServers1);
        registry.add("jeap.messaging.kafka.cluster.aws.bootstrapServers", embeddedKafka::getBootstrapServers2);
    }

    @Value("${local.server.port}")
    private int localServerPort;

    @Test
    void jeapKafkaHealthIndicator_multiCluster_reportsAllClustersUp() throws Exception {
        HttpURLConnection conn = openLocalConnection("/actuator/health/jeapKafka");
        assertThat(conn.getResponseCode()).isEqualTo(200);
        JsonNode body = OBJECT_MAPPER.readTree(conn.getInputStream().readAllBytes());
        assertThat(body.path("status").asText()).isEqualTo("UP");
        assertThat(body.path("details").path("default").path("status").asText()).isEqualTo("UP");
        assertThat(body.path("details").path("default").path("details").path("clusterId").asText()).isNotBlank();
        assertThat(body.path("details").path("default").path("details").path("nodeCount").asInt()).isEqualTo(1);
        assertThat(body.path("details").path("aws").path("status").asText()).isEqualTo("UP");
        assertThat(body.path("details").path("aws").path("details").path("clusterId").asText()).isNotBlank();
        assertThat(body.path("details").path("aws").path("details").path("nodeCount").asInt()).isEqualTo(1);
    }

    @Test
    void actuatorHealth_includesJeapKafkaComponent() throws Exception {
        HttpURLConnection conn = openLocalConnection("/actuator/health");
        assertThat(conn.getResponseCode()).isEqualTo(200);
        JsonNode body = OBJECT_MAPPER.readTree(conn.getInputStream().readAllBytes());
        assertThat(body.path("status").asText()).isEqualTo("UP");
        assertThat(body.path("components").path("jeapKafka").path("status").asText()).isEqualTo("UP");
    }

    private HttpURLConnection openLocalConnection(String path) throws Exception {
        URI uri = URI.create("http://127.0.0.1:" + localServerPort + path);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection(Proxy.NO_PROXY);
        conn.setRequestMethod("GET");
        return conn;
    }
}
