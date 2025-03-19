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

    public static SequentialInboxConfigurationException messageTypeNotConfiguredInAnySequence(String messageTypeName) {
        return new SequentialInboxConfigurationException(
                "Message type %s is not configured in any message sequence configuration, but has been received in the sequenced message listener"
                        .formatted(messageTypeName));
    }

    public static SequentialInboxConfigurationException missingSequenceName() {
        return new SequentialInboxConfigurationException(
                "The sequential inbox configuration contains a sequence without a type attribute");
    }

    public static SequentialInboxConfigurationException emptySequence(String name) {
        return new SequentialInboxConfigurationException(
                "The sequential inbox configuration contains an empty sequence (sequence name: %s)".formatted(name));
    }

    public static SequentialInboxConfigurationException missingMessageType(String sequenceName) {
        return new SequentialInboxConfigurationException(
                "The sequential inbox configuration contains a message without a type attribute in sequence %s".formatted(sequenceName));
    }

    public static SequentialInboxConfigurationException missingContextIdExtractor(String sequenceName) {
        return new SequentialInboxConfigurationException(
                "The sequential inbox configuration contains a message without a contextIdExtractor in sequence %s".formatted(sequenceName));
    }

    public static SequentialInboxConfigurationException retentionPeriodMissing(String sequenceName) {
        return new SequentialInboxConfigurationException("Missing required retention period for sequence name: " + sequenceName);
    }

    public static SequentialInboxConfigurationException mixedTypeAndSubtype(Set<String> types) {
        return new SequentialInboxConfigurationException("Mix of configuration of messages type with and without subtype: " + types);
    }

    public static SequentialInboxConfigurationException missingSubTypeResolver(String type) {
        return new SequentialInboxConfigurationException("Missing subtype resolver for message type which has a subtype defined: " + type);
    }

    public static SequentialInboxConfigurationException subtypeResolverForBadType(String type) {
        return new SequentialInboxConfigurationException("Subtype resolver for message type that is either not sequenced or has not subtypes defined: " + type);
    }

    public static SequentialInboxConfigurationException unknownSubtype(String type, Set<String> configuredSubTypes, Set<String> enumSubTypes) {
        return new SequentialInboxConfigurationException("Unknown subtype for type %s. Configured subtypes: %s. Valid subtypes: %s"
                .formatted(type, configuredSubTypes, enumSubTypes));
    }

    public static SequentialInboxConfigurationException missingSubType(String type, Set<String> configuredSubTypes, Set<String> enumSubTypes) {
        return new SequentialInboxConfigurationException("Missing subtype for type %s. Configured subtypes: %s. Valid subtypes: %s"
                .formatted(type, configuredSubTypes, enumSubTypes));
    }

    public static SequentialInboxConfigurationException badInstanceType(Class<?> clazz, Class<?> expectedSupertype) {
        return new SequentialInboxConfigurationException("Class %s is not a subtype of %s".formatted(clazz, expectedSupertype));
    }

    public static SequentialInboxConfigurationException inconsistentTopicNames(String messageType, Set<String> topics) {
        return new SequentialInboxConfigurationException("Different topics configured for subtypes of message type %s: %s".formatted(messageType, topics));
    }
}
