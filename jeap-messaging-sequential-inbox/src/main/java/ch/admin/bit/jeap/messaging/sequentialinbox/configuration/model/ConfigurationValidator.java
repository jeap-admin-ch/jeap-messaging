package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;


import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer.SequentialInboxConfigurationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static java.util.stream.Collectors.*;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
class ConfigurationValidator {

    private static final String DEFAULT_MESSAGE_TYPE_TOPIC = "<default message type topic>";

    private record TypeWithSubtype(String type, String subType) {
    }

    private final List<Sequence> sequences;
    private final Map<String, SubTypeResolver<AvroMessage, ?>> subTypeResolvers;

    static void validate(List<Sequence> sequences, Map<String, SubTypeResolver<AvroMessage, ?>> subTypeResolvers) {
        var validator = new ConfigurationValidator(sequences, subTypeResolvers);
        validator.validate();
    }

    private void validate() {
        checkDuplicatedMessageTypesAndTopics();
        checkRetentionPeriod();
        sequences.forEach(ConfigurationValidator::validateSequence);
        validateSubtypes();

    }

    private void checkRetentionPeriod() {
        sequences.forEach(sequence -> {
            if (sequence.getRetentionPeriod() == null || sequence.getRetentionPeriod().toMinutes() == 0) {
                throw SequentialInboxConfigurationException.retentionPeriodMissing(sequence.getName());
            }
        });
    }

    private static void validateSequence(Sequence sequence) {
        if (!hasText(sequence.getName())) {
            throw SequentialInboxConfigurationException.missingSequenceName();
        }
        if (sequence.getMessages() == null || sequence.getMessages().isEmpty()) {
            throw SequentialInboxConfigurationException.emptySequence(sequence.getName());
        }

        sequence.getMessages().forEach(message ->
                validateSequencedMessageType(sequence.getName(), message));

        ReleaseConditionsValidator validator = new ReleaseConditionsValidator(sequence.getMessages());
        validator.validate();
    }

    private static void validateSequencedMessageType(String sequenceName, SequencedMessageType sequencedMessageType) {
        if (!StringUtils.hasText(sequencedMessageType.getQualifiedName())) {
            throw SequentialInboxConfigurationException.missingMessageType(sequenceName);
        }
        if (sequencedMessageType.getContextIdExtractor() == null) {
            throw SequentialInboxConfigurationException.missingContextIdExtractor(sequenceName);
        }
    }

    private void checkDuplicatedMessageTypesAndTopics() {
        Set<String> messageTypeQualifiedNames = new HashSet<>();
        Set<String> duplicatedMessageTypes = new HashSet<>();

        sequences.forEach(sequence -> sequence.getMessages().forEach(message -> {
            String qualifiedName = message.getQualifiedName();
            if (!messageTypeQualifiedNames.add(qualifiedName)) {
                duplicatedMessageTypes.add(qualifiedName);
            }
        }));

        validateAllSubtypesHaveSameTopicConfigured();

        if (!duplicatedMessageTypes.isEmpty()) {
            throw SequentialInboxConfigurationException.duplicatedMessageTypes(duplicatedMessageTypes);
        }
    }

    private void validateAllSubtypesHaveSameTopicConfigured() {
        Map<String, Set<String>> topicsByJeapMessageType = sequences.stream()
                .flatMap(s -> s.getMessages().stream())
                .filter(s -> s.getJeapMessageTypeName() != null)
                .collect(groupingBy(
                        SequencedMessageType::getJeapMessageTypeName,
                        mapping(this::topicNameOrDefaultTopic, toSet())));
        topicsByJeapMessageType.forEach((messageType, topics) -> {
            if (topics.size() > 1) {
                throw SequentialInboxConfigurationException.inconsistentTopicNames(messageType, topics);
            }
        });

        Set<String> topics = new HashSet<>();
        Set<String> duplicatedTopics = new HashSet<>();
        topicsByJeapMessageType.values().stream()
                .flatMap(Set::stream)
                .filter(topic -> !DEFAULT_MESSAGE_TYPE_TOPIC.equals(topic))
                .forEach(topic -> {
                    if (!topics.add(topic)) {
                        duplicatedTopics.add(topic);
                    }
                });

        if (!duplicatedTopics.isEmpty()) {
            throw SequentialInboxConfigurationException.duplicatedTopics(duplicatedTopics);
        }
    }

    private String topicNameOrDefaultTopic(SequencedMessageType smt) {
        return smt.getTopic() == null ? DEFAULT_MESSAGE_TYPE_TOPIC : smt.getTopic();
    }

    private void validateSubtypes() {
        Collection<SequencedMessageType> messages = sequences.stream()
                .flatMap(sequence -> sequence.getMessages().stream())
                .toList();

        validateTypeAndSubtypeNotMixed(messages);
        validateSubTypeResolvers(messages);
    }

    private void validateSubTypeResolvers(Collection<SequencedMessageType> messages) {
        // Make sure every type with a subtype has a corresponding resolver
        List<TypeWithSubtype> typesWithSubtypes = messages.stream()
                .filter(ConfigurationValidator::hasSubtype)
                .map(smt -> new TypeWithSubtype(smt.getJeapMessageTypeName(), smt.getSubType()))
                .toList();
        typesWithSubtypes.forEach(typeWithSubtype -> {
            if (!subTypeResolvers.containsKey(typeWithSubtype.type())) {
                throw SequentialInboxConfigurationException.missingSubTypeResolver(typeWithSubtype.type());
            }
        });

        validateAllSubtypesResolvable(messages, typesWithSubtypes);
    }

    private void validateAllSubtypesResolvable(Collection<SequencedMessageType> messages, List<TypeWithSubtype> typesWithSubtypes) {
        Set<String> messageTypesWithSubtype = messageTypesWithSubtypes(messages);
        Map<String, List<TypeWithSubtype>> typeWithSubtypeByMessageType = typesWithSubtypes.stream()
                .collect(groupingBy(TypeWithSubtype::type));

        subTypeResolvers.forEach((type, resolver) -> {
            if (!messageTypesWithSubtype.contains(type)) {
                throw SequentialInboxConfigurationException.subtypeResolverForBadType(type);
            }
            Set<String> configuredSubTypes = typeWithSubtypeByMessageType.get(type).stream()
                    .map(TypeWithSubtype::subType)
                    .collect(toSet());

            validateSubTypeEnumConfiguration(type, resolver, configuredSubTypes);
        });
    }

    private static void validateSubTypeEnumConfiguration(String type, SubTypeResolver<?, ?> resolver, Set<String> configuredSubTypes) {
        Class<Enum<?>> subTypeEnum = getSubTypeEnumReturnedBySubtypeResolver(resolver);
        Set<String> enumSubTypes = Arrays.stream(subTypeEnum.getEnumConstants())
                .map(Enum::name)
                .collect(toSet());

        // Check if all configured subtypes are valid enum values
        if (!enumSubTypes.containsAll(configuredSubTypes)) {
            throw SequentialInboxConfigurationException.unknownSubtype(type, configuredSubTypes, enumSubTypes);
        }
        // Check if a subtype is configured for all enum values
        if (!configuredSubTypes.containsAll(enumSubTypes)) {
            throw SequentialInboxConfigurationException.missingSubType(type, configuredSubTypes, enumSubTypes);
        }
    }

    private void validateTypeAndSubtypeNotMixed(Collection<SequencedMessageType> messages) {
        Set<String> messageTypesWithSubtype = messageTypesWithSubtypes(messages);
        Set<String> messageTypesWithoutSubtype = messages.stream()
                .filter(smt -> !hasSubtype(smt))
                .map(SequencedMessageType::getJeapMessageTypeName)
                .collect(toSet());
        // The two sets should be disjoint - a type should either be configured with subtypes or without, but not both
        messageTypesWithSubtype.retainAll(messageTypesWithoutSubtype);
        if (!messageTypesWithSubtype.isEmpty()) {
            throw SequentialInboxConfigurationException.mixedTypeAndSubtype(messageTypesWithSubtype);
        }
    }

    private static HashSet<String> messageTypesWithSubtypes(Collection<SequencedMessageType> messages) {
        return messages.stream()
                .filter(ConfigurationValidator::hasSubtype)
                .map(SequencedMessageType::getJeapMessageTypeName)
                .collect(toCollection(HashSet::new));
    }

    private static boolean hasSubtype(SequencedMessageType smt) {
        return !smt.getJeapMessageTypeName().equals(smt.getQualifiedName());
    }

    @SuppressWarnings({"unchecked", "java:S1452"})
    static Class<Enum<?>> getSubTypeEnumReturnedBySubtypeResolver(SubTypeResolver<?, ?> resolver) {
        Class<Enum<?>> enumType = null;
        Type[] genericInterfaces = resolver.getClass().getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                Type rawType = parameterizedType.getRawType();
                if (rawType.getTypeName().equals(SubTypeResolver.class.getName())) {
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length > 1) {
                        // Second type arg is the enum: SubTypeResolver<M extends AvroMessage, E extends Enum<E>>
                        enumType = (Class<Enum<?>>) actualTypeArguments[1];
                        break;
                    }
                }
            }
        }
        if (enumType == null || !enumType.isEnum()) {
            throw new IllegalStateException("Unexpected: Unable to resolve enum type from SubTypeResolver");
        }
        return enumType;
    }
}
