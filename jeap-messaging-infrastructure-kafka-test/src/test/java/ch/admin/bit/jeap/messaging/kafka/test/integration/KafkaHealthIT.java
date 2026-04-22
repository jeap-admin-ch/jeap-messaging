package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.application.name=jme-messaging-subscriber-service",
        "management.endpoints.web.exposure.include=health",
        "management.endpoint.health.show-details=always"
})
@DirtiesContext
class KafkaHealthIT extends KafkaIntegrationTestBase {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @MockitoBean
    private ContractsValidator contractsValidator;

    @Value("${local.server.port}")
    private int localServerPort;

    @Test
    void jeapKafkaHealthIndicator_singleCluster_reportsUp() throws Exception {
        HttpURLConnection conn = openLocalConnection("/actuator/health/jeapKafka");
        assertThat(conn.getResponseCode()).isEqualTo(200);
        JsonNode body = OBJECT_MAPPER.readTree(conn.getInputStream().readAllBytes());
        assertThat(body.path("status").asText()).isEqualTo("UP");
        assertThat(body.path("details").path("default").path("status").asText()).isEqualTo("UP");
        assertThat(body.path("details").path("default").path("details").path("clusterId").asText()).isNotBlank();
        assertThat(body.path("details").path("default").path("details").path("nodeCount").asInt()).isEqualTo(1);
    }

    @Test
    void actuatorHealth_includesJeapKafkaComponent() throws Exception {
        HttpURLConnection conn = openLocalConnection("/actuator/health");
        assertThat(conn.getResponseCode()).isEqualTo(200);
        JsonNode body = OBJECT_MAPPER.readTree(conn.getInputStream().readAllBytes());
        assertThat(body.path("status").asText()).isEqualTo("UP");
        assertThat(body.path("components").path("jeapKafka").path("status").asText()).isEqualTo("UP");
    }

    @Test
    void springBootKafkaHealthIndicator_isDisabledByDefault() throws Exception {
        HttpURLConnection conn = openLocalConnection("/actuator/health/kafka");
        assertThat(conn.getResponseCode()).isEqualTo(404);
    }

    private HttpURLConnection openLocalConnection(String path) throws Exception {
        URI uri = URI.create("http://127.0.0.1:" + localServerPort + path);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection(Proxy.NO_PROXY);
        conn.setRequestMethod("GET");
        return conn;
    }
}
