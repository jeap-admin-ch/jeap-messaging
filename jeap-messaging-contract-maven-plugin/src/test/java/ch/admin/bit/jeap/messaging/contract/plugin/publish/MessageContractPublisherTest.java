package ch.admin.bit.jeap.messaging.contract.plugin.publish;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WireMockTest(httpPort = 40169)
class MessageContractPublisherTest {

    private static final String RESOURCE_PATH = "/api/contracts/test-app/1.0";

    private static final String RESOURCE_PATH_WITH_TRANSACTION_ID = "/api/contracts/test-app/1.0?transactionId=tx1";

    private final Log log = new SystemStreamLog();

    @Test
    void publishMessageContracts(WireMockRuntimeInfo wireMock) {
        stubFor(put(RESOURCE_PATH).willReturn(created()));
        String baseUri = "http://localhost:" + wireMock.getHttpPort();
        MessageContractServiceClient client = new MessageContractServiceClient(baseUri, log, "user", "secret");
        MessageContractPublisher messageContractPublisher = new MessageContractPublisher(client, log);
        Path contractsPath = Path.of("src/test/resources/validContracts");

        messageContractPublisher.publishMessageContracts(contractsPath, "1.0", null);

        verify(putRequestedFor(urlEqualTo(RESOURCE_PATH))
                .withBasicAuth(new BasicCredentials("user", "secret"))
                .withRequestBody(and(
                        // Expect five contracts
                        matchingJsonPath("$.contracts.length()", equalTo("5")),
                        // Expect four producer and one consumer contract
                        matchingJsonPath("$.contracts[?(@.topic=='test-producer-topic-1')]"),
                        matchingJsonPath("$.contracts[?(@.topic=='test-producer-topic-2')]"),
                        matchingJsonPath("$.contracts[?(@.topic=='test-producer-topic-encrypted')]"),
                        matchingJsonPath("$.contracts[?(@.topic=='test-producer-topic-not-encrypted')]"),
                        matchingJsonPath("$.contracts[?(@.topic=='test-consumer-topic')]"))));
    }

    @Test
    void publishMessageContractsWithTransactionId(WireMockRuntimeInfo wireMock) {
        stubFor(put(RESOURCE_PATH_WITH_TRANSACTION_ID).willReturn(created()));
        String baseUri = "http://localhost:" + wireMock.getHttpPort();
        MessageContractServiceClient client = new MessageContractServiceClient(baseUri, log, "user", "secret");
        MessageContractPublisher messageContractPublisher = new MessageContractPublisher(client, log);
        Path contractsPath = Path.of("src/test/resources/validContracts");

        messageContractPublisher.publishMessageContracts(contractsPath, "1.0", "tx1");

        verify(putRequestedFor(urlEqualTo(RESOURCE_PATH_WITH_TRANSACTION_ID))
                .withBasicAuth(new BasicCredentials("user", "secret"))
                .withRequestBody(and(
                        // Expect five contracts
                        matchingJsonPath("$.contracts.length()", equalTo("5")),
                        // Expect four producer and one consumer contract
                        matchingJsonPath("$.contracts[?(@.topic=='test-producer-topic-1')]"),
                        matchingJsonPath("$.contracts[?(@.topic=='test-producer-topic-2')]"),
                        matchingJsonPath("$.contracts[?(@.topic=='test-producer-topic-encrypted')]"),
                        matchingJsonPath("$.contracts[?(@.topic=='test-producer-topic-not-encrypted')]"),
                        matchingJsonPath("$.contracts[?(@.topic=='test-consumer-topic')]"))));
    }

    @Test
    void noContractsFound_shouldNotPublishAnyAndSucceed(WireMockRuntimeInfo wireMock) {
        MessageContractServiceClient client = new MessageContractServiceClient("uri", log, "user", "secret");
        MessageContractPublisher messageContractPublisher = new MessageContractPublisher(client, log);
        Path contractsPath = Path.of("does/not/exist");

        messageContractPublisher.publishMessageContracts(contractsPath, "1.0", "tx1");

        assertEquals(0, wireMock.getWireMock().getServeEvents().size(), "No requests made");
    }

    @Test
    void publishMessageContracts_badAppName_shouldFail() {
        MessageContractServiceClient client = new MessageContractServiceClient("uri", log, "user", "secret");
        MessageContractPublisher messageContractPublisher = new MessageContractPublisher(client, log);
        Path contractsPath = Path.of("src/test/resources/badAppNameContracts");

        MessageContractPublishException ex = assertThrows(MessageContractPublishException.class, () ->
                messageContractPublisher.publishMessageContracts(contractsPath, "1.0", "tx1"));

        assertThat(ex.getMessage())
                .contains("This maven project contains messaging contracts for different apps: invalid-app,test-app");
    }

    @Test
    void publishMessageContracts_invalidContractFile_shouldFail() {
        MessageContractServiceClient client = new MessageContractServiceClient("uri", log, "user", "secret");
        MessageContractPublisher messageContractPublisher = new MessageContractPublisher(client, log);
        Path contractsPath = Path.of("src/test/resources/invalidContract");

        MessageContractPublishException ex = assertThrows(MessageContractPublishException.class, () ->
                messageContractPublisher.publishMessageContracts(contractsPath, "1.0", "tx1"));

        assertThat(ex.getMessage())
                .contains("Failed to load messaging contracts");
    }

    @Test
    void publishMessageContracts_statusCode500_shouldTryThreeTimesThenFail(WireMockRuntimeInfo wireMock) {
        stubFor(put(RESOURCE_PATH).willReturn(serverError()));
        String baseUri = "http://localhost:" + wireMock.getHttpPort();
        MessageContractServiceClient client = new MessageContractServiceClient(baseUri, log, "user", "secret");
        MessageContractPublisher messageContractPublisher = new MessageContractPublisher(client, log);
        Path contractsPath = Path.of("src/test/resources/validContracts");

        assertThrows(MessageContractPublishException.class, () ->
                messageContractPublisher.publishMessageContracts(contractsPath, "1.0", null));

        verify(3, putRequestedFor(urlEqualTo(RESOURCE_PATH)));
    }

    @Test
    void publishMessageContracts_statusCode500Then200_shouldTryThreeTimesThenSucceed(WireMockRuntimeInfo wireMock) {
        stubFor(put(RESOURCE_PATH)
                .inScenario("Retry Scenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(serverError())
                .willSetStateTo("Next Request Succeeds"));
        stubFor(put(RESOURCE_PATH)
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Next Request Succeeds")
                .willReturn(ok()));

        String baseUri = "http://localhost:" + wireMock.getHttpPort() + "/";
        MessageContractServiceClient client = new MessageContractServiceClient(baseUri, log, "user", "secret");
        MessageContractPublisher messageContractPublisher = new MessageContractPublisher(client, log);
        Path contractsPath = Path.of("src/test/resources/validContracts");

        messageContractPublisher.publishMessageContracts(contractsPath, "1.0", null);

        verify(2, putRequestedFor(urlEqualTo(RESOURCE_PATH)));
    }
}
