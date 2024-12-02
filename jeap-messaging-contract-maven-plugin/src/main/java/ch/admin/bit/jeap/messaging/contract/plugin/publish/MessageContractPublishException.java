package ch.admin.bit.jeap.messaging.contract.plugin.publish;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Set;

import static java.util.stream.Collectors.joining;

class MessageContractPublishException extends RuntimeException {
    private MessageContractPublishException(String message) {
        super(message);
    }

    private MessageContractPublishException(String message, Throwable cause) {
        super(message, cause);
    }

    static MessageContractPublishException badMessageContractServiceResponseCode(HttpResponse<?> response) {
        URI uri = response.uri();
        int statusCode = response.statusCode();
        return new MessageContractPublishException("Failed to publish message contract to message contract service at " +
                uri + ", response code " + statusCode);
    }

    static MessageContractPublishException badMessageContractServiceResponse(String uri, Exception exception) {
        return new MessageContractPublishException("Failed to publish message contract to message contract service at " +
                uri + ": " + exception.getMessage(), exception);
    }

    static MessageContractPublishException interrupted() {
        return new MessageContractPublishException("Upload thread interrupted");
    }

    static MessageContractPublishException jsonSerializationFailure(Exception exception) {
        return new MessageContractPublishException("Failed to serialize json request body: " + exception.getMessage(), exception);
    }

    static MessageContractPublishException failedToLoadContracts(Path contractDirectory, Exception exception) {
        return new MessageContractPublishException("Failed to load messaging contracts from " + contractDirectory, exception);
    }

    static MessageContractPublishException ambiguousAppNames(Set<String> appNames) {
        String appNameString = appNames.stream()
                .sorted()
                .collect(joining(","));
        return new MessageContractPublishException("This maven project contains messaging contracts for different apps: " +
                appNameString +
                ". Please correct the messaging contracts in this project to contain a single consistent name for the " +
                "consumer/producer application in this project.");
    }

    static MessageContractPublishException contractDeserializationFailure(Path path, IOException exception) {
        return new MessageContractPublishException("Failed to read messaging contract at " + path + ": " + exception.getMessage(), exception);
    }

    static MessageContractPublishException badAppName(String appName) {
        return new MessageContractPublishException("Messaging contracts contain an invalid app name: " + appName +
                " - please check if your messaging contracts contain a valid app name definition.");
    }

    static MessageContractPublishException contractPathIsNotADirectory(Path contractPath) {
        return new MessageContractPublishException("The messaging contract location " + contractPath +
                " exists, but is not a directory.");
    }
}
