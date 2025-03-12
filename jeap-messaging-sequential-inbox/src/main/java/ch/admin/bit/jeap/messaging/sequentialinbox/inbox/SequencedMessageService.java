package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.ClusterNameHeaderInterceptor;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.Sequence;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequencedMessageType;
import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.MessageRepository;
import ch.admin.bit.jeap.messaging.sequentialinbox.kafka.TraceContextFactory;
import ch.admin.bit.jeap.messaging.sequentialinbox.metrics.SequentialInboxMetricsCollector;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.BufferedMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequenceInstance;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessageState;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class SequencedMessageService {

    private final TraceContextFactory traceContextFactory;
    private final MessageRepository messageRepository;
    private final KafkaProperties kafkaProperties;
    private final SequentialInboxMetricsCollector metricsCollector;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void storeSequencedMessage(Optional<SequencedMessage> existingSequencedMessage,
                                      SequenceInstance sequenceInstance,
                                      SequencedMessageState state,
                                      ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord) {

        String messageType = consumerRecord.value().getType().getName();
        if (existingSequencedMessage.isPresent()) {
            messageRepository.setMessageStateInCurrentTransaction(existingSequencedMessage.get(), state);
            return;
        }

        metricsCollector.onConsumedSequencedMessage(messageType);

        BufferedMessage bufferedMessage = null;
        if (state == SequencedMessageState.WAITING) {
            bufferedMessage = BufferedMessage.builder()
                    .sequenceInstanceId(sequenceInstance.getId())
                    .key(consumerRecord.key() == null ? null : consumerRecord.key().getSerializedMessage())
                    .value(consumerRecord.value().getSerializedMessage())
                    .build();
        }

        String clusterName = getClusterName(consumerRecord);

        SequencedMessage sequencedMessage = SequencedMessage.builder()
                .messageType(messageType)
                .sequenceInstanceId(sequenceInstance.getId())
                .clusterName(clusterName)
                .topic(consumerRecord.topic())
                .sequencedMessageId(UUID.fromString(consumerRecord.value().getIdentity().getId()))
                .idempotenceId(consumerRecord.value().getIdentity().getIdempotenceId())
                .traceContext(traceContextFactory.currentTraceContext())
                .state(state)
                .build();

        messageRepository.saveMessage(bufferedMessage, sequencedMessage);
    }

    private String getClusterName(ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord) {
        String clusterName = ClusterNameHeaderInterceptor.getClusterName(consumerRecord);
        return clusterName == null ? kafkaProperties.getDefaultClusterName() : clusterName;
    }

    Optional<SequencedMessage> findByMessageTypeAndIdempotenceId(String messageType, String idempotenceId) {
        return messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction(messageType, idempotenceId);
    }

    boolean isReleaseConditionSatisfied(SequencedMessageType sequencedMessageType, SequenceInstance sequenceInstance) {
        // Avoid querying the database if there is no release condition. In case the message does not have a release
        // condition (first message in a sequence), it should be processed immediately.
        Set<String> emptyProcessedMessageSet = Set.of();
        if (sequencedMessageType.isReleaseConditionSatisfied(emptyProcessedMessageSet)) {
            return true;
        }

        Set<String> processedMessageTypes = messageRepository.getProcessedMessageTypesInSequenceInNewTransaction(sequenceInstance);
        return sequencedMessageType.isReleaseConditionSatisfied(processedMessageTypes);
    }

    boolean areAllMessagesProcessed(Sequence sequence, SequenceInstance sequenceInstance) {
        Set<String> allMessageTypeNames = sequence.getMessageTypeNames();
        Set<String> processedMessageTypes = messageRepository.getProcessedMessageTypesInSequenceInNewTransaction(sequenceInstance);
        return allMessageTypeNames.equals(processedMessageTypes);
    }
}
