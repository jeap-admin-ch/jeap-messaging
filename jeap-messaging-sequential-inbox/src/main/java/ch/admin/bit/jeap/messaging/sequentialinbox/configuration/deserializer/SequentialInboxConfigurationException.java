package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import java.io.IOException;
import java.util.Set;

public class SequentialInboxConfigurationException extends RuntimeException {

    private SequentialInboxConfigurationException(String message) {
        super(message);
    }

    private SequentialInboxConfigurationException(String message, Exception cause) {
        super(message, cause);
    }

    public static SequentialInboxConfigurationException errorWhileCreatingInstance(String className, Exception cause) {
        return new SequentialInboxConfigurationException("Error while creating instance of " + className, cause);
    }

    public static SequentialInboxConfigurationException duplicatedMessageTypes(Set<String> list) {
        return new SequentialInboxConfigurationException("Duplicated message types: " + list);
    }

    public static SequentialInboxConfigurationException duplicatedTopics(Set<String> list) {
        return new SequentialInboxConfigurationException("Duplicated topics: " + list);
    }

    public static SequentialInboxConfigurationException predecessorNotFound(String predecessor) {
        return new SequentialInboxConfigurationException("Predecessor not found: " + predecessor);
    }

    public static SequentialInboxConfigurationException circularPredecessorDefinition(String messageType, String predecessor) {
        return new SequentialInboxConfigurationException("Circular predecessor definition: " + messageType + " -> " + predecessor + " -> " + messageType);
    }

    public static SequentialInboxConfigurationException invalidPredecessorConfiguration(String messageType) {
        return new SequentialInboxConfigurationException("Invalid predecessor configuration: " + messageType);
    }

    public static SequentialInboxConfigurationException configurationFileLoadingError(String location, IOException e) {
        return new SequentialInboxConfigurationException("Error while loading configuration file: " + location, e);
    }

    public static SequentialInboxConfigurationException configurationFileParsingError(String location, IOException e) {
        return new SequentialInboxConfigurationException("Error while parsing configuration file: " + location, e);
    }

    public static SequentialInboxConfigurationException duplicatedPredecessor(String messageType) {
        return new SequentialInboxConfigurationException("Duplicated predecessor in operation: " + messageType);
    }
}
