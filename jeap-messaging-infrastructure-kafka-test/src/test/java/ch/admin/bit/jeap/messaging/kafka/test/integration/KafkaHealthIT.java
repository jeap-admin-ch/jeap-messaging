package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.application.name=jme-messaging-subscriber-service",
        "management.endpoints.web.exposure.include=health",
        "management.endpoint.health.show-details=always"
})
@DirtiesContext
class KafkaHealthIT extends KafkaIntegrationTestBase {

    @MockitoBean
    private ContractsValidator contractsValidator;

    @LocalServerPort
    private int localServerPort;

    @Test
    void jeapKafkaHealthIndicator_singleCluster_reportsUp() {
        RestAssured.given().port(localServerPort)
                .get("/actuator/health/jeapKafka")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("details.default.status", equalTo("UP"))
                .body("details.default.details.clusterId", notNullValue())
                .body("details.default.details.nodeCount", equalTo(1));
    }

    @Test
    void actuatorHealth_includesJeapKafkaComponent() {
        RestAssured.given().port(localServerPort)
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("components.jeapKafka.status", equalTo("UP"));
    }

    @Test
    void springBootKafkaHealthIndicator_isDisabledByDefault() {
        RestAssured.given().port(localServerPort)
                .get("/actuator/health/kafka")
                .then()
                .statusCode(404);
    }
}
