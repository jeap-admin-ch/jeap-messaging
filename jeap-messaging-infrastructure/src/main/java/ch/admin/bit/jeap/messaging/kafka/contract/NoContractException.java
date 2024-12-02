package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.model.MessageType;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class NoContractException extends RuntimeException {
    private NoContractException(String message) {
        super(message);
    }

    private NoContractException(String message, Throwable cause) {
        super(message, cause);
    }

    public static NoContractException noContract(String appName, String role, MessageType type, String topic) {
        String message = String.format("Application %s does not have a contract as %s for messages of type %s on topic %s.",
                appName, role, type.toString(), topic);
        return new NoContractException(message);
    }

    public static NoContractException noContract(String appName, MessageType type) {
        String message = String.format("Application %s does not have a contract for events of type %s",
                appName, type.toString());
        return new NoContractException(message);
    }

    public static NoContractException consumeNotAllowed(String appName, MessageType type, List<String> allowedApplications) {
        String message = String.format("Application %s is not allowed to consume events of type %s, " +
                        "the contract allows only applications %s to do so",
                appName, type.toString(), allowedApplications);
        return new NoContractException(message);
    }

    public static NoContractException publishNotAllowed(String appName, MessageType type, List<String> allowedApplications) {
        String message = String.format("Application %s is not allowed to publish events of type %s, " +
                        "the contract allows only applications %s to do so",
                appName, type.toString(), allowedApplications);
        return new NoContractException(message);
    }

    public static NoContractException cannotReadContractFile(Resource contractFile, IOException e) {
        String message = String.format("Cannot read contract file %s", contractFile.getDescription());
        return new NoContractException(message, e);
    }

    public static NoContractException cannotReadContracts(String contractLocation, IOException e) {
        String message = String.format("Reading contracts from '%s' failed.", contractLocation);
        return new NoContractException(message, e);
    }

}
