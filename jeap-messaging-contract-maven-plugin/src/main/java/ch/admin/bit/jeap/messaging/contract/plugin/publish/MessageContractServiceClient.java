package ch.admin.bit.jeap.messaging.contract.plugin.publish;

import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class MessageContractServiceClient {

    private final String messageContractServiceBaseUri;
    private final Log log;
    private final String username;
    private final String password;

    public MessageContractServiceClient(String messageContractServiceBaseUri, Log log, String username, String password) {
        this.messageContractServiceBaseUri = removeTrailingSlash(messageContractServiceBaseUri);
        this.log = log;
        this.username = username;
        this.password = password;
    }

    private String removeTrailingSlash(String uri) {
        if (uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    void putContracts(String appName, String appVersion, String jsonBody, String transactionId) {
        retry(3, () -> tryPutContract(appName, appVersion, jsonBody, transactionId));
    }

    private void tryPutContract(String appName, String appVersion, String jsonBody, String transactionId) {
        String uri = messageContractServiceBaseUri + "/api/contracts/" + appName + "/" + appVersion;
        if (transactionId != null) {
            uri += "?transactionId=" + transactionId;
        }
        log.info("Publishing message contracts for app " + appName + " version " + appVersion + " to " + uri);
        HttpResponse<?> response = httpPut(uri, jsonBody);
        handleResponseStatusCode(response);
    }

    private void handleResponseStatusCode(HttpResponse<?> response) {
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode > 204) {
            log.warn("Failed to publish message contract to message contract service, response code " + statusCode);
            throw MessageContractPublishException.badMessageContractServiceResponseCode(response);
        }
    }

    private HttpResponse<?> httpPut(String uri, String jsonBody) {
        HttpClient client = HttpClient.newBuilder()
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Authorization", basicAuth())
                .header("Content-Type", "application/json")
                .build();

        try {
            return client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw MessageContractPublishException.interrupted();
        } catch (Exception e) {
            log.error("Failed to publish message contract to message contract service at " + uri + ": " + e.getMessage());
            throw MessageContractPublishException.badMessageContractServiceResponse(uri, e);
        }
    }

    private String basicAuth() {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    @SneakyThrows
    private void retry(int nTimes, Runnable statement) {
        boolean success = false;
        int attemptNumber = 0;
        do {
            try {
                attemptNumber++;
                statement.run();
                success = true;
            } catch (Exception ex) {
                String message = "Failed to publish message contract to message contract service, retrying: " + ex.getMessage();
                if (attemptNumber == nTimes) {
                    log.error(message, ex);
                    throw ex;
                } else {
                    log.debug(message, ex);
                    Thread.sleep(3000);
                }
            }
        } while (!success);
    }
}
