package ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster;

import ch.admin.bit.jeap.messaging.kafka.test.EmbeddedKafkaMultiClusterExtension;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster.KafkaHealthMultiClusterIT.PORT_OFFSET;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.application.name=jme-messaging-receiverpublisher-service",
        "management.endpoints.web.exposure.include=health",
        "management.endpoint.health.show-details=always",
        "jeap.messaging.kafka.embedded=false",
        "jeap.messaging.kafka.systemName=test",
        "jeap.messaging.kafka.errorTopicName=errorTopic",
        "jeap.messaging.kafka.message-type-encryption-disabled=true",
        "jeap.messaging.kafka.cluster.default.bootstrapServers=localhost:" + (EmbeddedKafkaMultiClusterExtension.BASE_PORT + PORT_OFFSET),
        "jeap.messaging.kafka.cluster.default.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.default.schemaRegistryUrl=mock://health-multi-cluster-registry-1",
        "jeap.messaging.kafka.cluster.default.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.default.schemaRegistryPassword=unused",
        "jeap.messaging.kafka.cluster.aws.bootstrapServers=localhost:" + (EmbeddedKafkaMultiClusterExtension.BASE_PORT + PORT_OFFSET + 1),
        "jeap.messaging.kafka.cluster.aws.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.cluster.aws.schemaRegistryUrl=mock://health-multi-cluster-registry-2",
        "jeap.messaging.kafka.cluster.aws.schemaRegistryUsername=unused",
        "jeap.messaging.kafka.cluster.aws.schemaRegistryPassword=unused"
})
@DirtiesContext
class KafkaHealthMultiClusterIT {

    static final int PORT_OFFSET = 30;

    @RegisterExtension
    static EmbeddedKafkaMultiClusterExtension embeddedKafka =
            EmbeddedKafkaMultiClusterExtension.withPortOffset(PORT_OFFSET);

    @LocalServerPort
    private int localServerPort;

    @Test
    void jeapKafkaHealthIndicator_multiCluster_reportsAllClustersUp() {
        RestAssured.given().port(localServerPort)
                .get("/actuator/health/jeapKafka")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("details.default.status", equalTo("UP"))
                .body("details.default.details.clusterId", notNullValue())
                .body("details.default.details.nodeCount", equalTo(1))
                .body("details.aws.status", equalTo("UP"))
                .body("details.aws.details.clusterId", notNullValue())
                .body("details.aws.details.nodeCount", equalTo(1));
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
}
