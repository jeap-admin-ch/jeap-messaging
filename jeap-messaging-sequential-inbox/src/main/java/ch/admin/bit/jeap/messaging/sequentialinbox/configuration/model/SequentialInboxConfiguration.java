package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer.SequentialInboxConfigurationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Slf4j
public class SequentialInboxConfiguration {

    @JsonProperty("sequences")
    private List<Sequence> sequences;
    @JsonProperty("subTypeResolvers")
    private Map<String, SubTypeResolver<AvroMessage, ?>> subTypeResolvers;

    private Map<String, Sequence> sequenceByQualifiedName;
    private Map<String, SequencedMessageType> sequencedMessageTypeByQualifiedName;

    public void validateAndInitialize() {
        if (subTypeResolvers == null) {
            subTypeResolvers = Map.of();
        }

        ConfigurationValidator.validate(sequences, subTypeResolvers);

        sequenceByQualifiedName = new HashMap<>();
        sequencedMessageTypeByQualifiedName = new HashMap<>();
        for (Sequence sequence : sequences) {
            sequence.init();
            for (SequencedMessageType message : sequence.getMessages()) {
                sequenceByQualifiedName.put(message.getQualifiedName(), sequence);
                sequencedMessageTypeByQualifiedName.put(message.getQualifiedName(), message);
            }
        }
    }

    public Sequence getSequenceByQualifiedSequencedMessageTypeName(String sequencedMessageTypeQn) {
        return sequenceByQualifiedName.get(sequencedMessageTypeQn);
    }

    public SequencedMessageType requireSequencedMessageTypeByQualifiedName(String sequencedMessageTypeQn) {
        SequencedMessageType sequencedMessageType = sequencedMessageTypeByQualifiedName.get(sequencedMessageTypeQn);
        if (sequencedMessageType == null) {
            throw SequentialInboxConfigurationException.messageTypeNotConfiguredInAnySequence(sequencedMessageTypeQn);
        }
        return sequencedMessageType;
    }

    public Set<SequencedMessageType> getSequencedMessageTypes() {
        return sequences.stream()
                .flatMap(sequence -> sequence.getMessages().stream())
                .collect(toSet());
    }

    public int getSequenceCount() {
        return sequences.size();
    }

    public String qualifiedSequencedMessageTypeName(AvroMessage avroMessage) {
        String subType = resolveSubtypeIfConfigured(avroMessage);
        return avroMessage.getType().getName() + (subType == null ? "" : ("." + subType));
    }

    private String resolveSubtypeIfConfigured(AvroMessage avroMessage) {
        String name = avroMessage.getType().getName();
        SubTypeResolver<AvroMessage, ?> subTypeResolver = subTypeResolvers.get(name);
        if (subTypeResolver == null) {
            return null;
        }
        return subTypeResolver.resolveSubType(avroMessage).name();
    }

    @Override
    public String toString() {
        return sequences.toString();
    }
}
