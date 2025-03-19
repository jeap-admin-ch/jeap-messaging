package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.ClusterNameHeaderInterceptor;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureHeaders;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.Sequence;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequencedMessageType;
import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.MessageRepository;
import ch.admin.bit.jeap.messaging.sequentialinbox.kafka.TraceContextFactory;
import ch.admin.bit.jeap.messaging.sequentialinbox.metrics.SequentialInboxMetricsCollector;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.BufferedMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.MessageHeader;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessageState;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
class SequencedMessageService {

    private final TraceContextFactory traceContextFactory;
    private final MessageRepository messageRepository;
    private final KafkaProperties kafkaProperties;
    private final SequentialInboxMetricsCollector metricsCollector;

    private static final List<String> PRESERVED_HEADER_NAMES = List.of(
            JeapKafkaAvroSerdeCryptoConfig.ENCRYPTED_VALUE_HEADER_NAME,
            SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY,
            SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY,
            SignatureHeaders.SIGNATURE_KEY_HEADER_KEY);

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void storeSequencedMessage(String messageTypeQualifiedName,
                                      Optional<SequencedMessage> existingSequencedMessage,
                                      long sequenceInstanceId,
                                      SequencedMessageState state,
                                      ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord) {

        if (existingSequencedMessage.isPresent()) {
            messageRepository.setMessageStateInCurrentTransaction(existingSequencedMessage.get(), state);
            return;
        }

        metricsCollector.onConsumedSequencedMessage(messageTypeQualifiedName);

        BufferedMessage bufferedMessage = null;
        if (state == SequencedMessageState.WAITING) {
            bufferedMessage = BufferedMessage.builder()
                    .sequenceInstanceId(sequenceInstanceId)
                    .key(consumerRecord.key() == null ? null : consumerRecord.key().getSerializedMessage())
                    .value(consumerRecord.value().getSerializedMessage())
                     .build();
            bufferedMessage.setHeaders(getMessageHeaders(consumerRecord.headers(), bufferedMessage));
        }

        String clusterName = getClusterName(consumerRecord);

        SequencedMessage sequencedMessage = SequencedMessage.builder()
                .messageType(messageTypeQualifiedName)
                .sequenceInstanceId(sequenceInstanceId)
                .clusterName(clusterName)
                .topic(consumerRecord.topic())
                .sequencedMessageId(UUID.fromString(consumerRecord.value().getIdentity().getId()))
                .idempotenceId(consumerRecord.value().getIdentity().getIdempotenceId())
                .traceContext(traceContextFactory.currentTraceContext())
                .state(state)
                .build();

        messageRepository.saveMessage(bufferedMessage, sequencedMessage);
    }

    private List<MessageHeader> getMessageHeaders(Headers headers, BufferedMessage bufferedMessage) {
        List<MessageHeader> messageHeaders = new ArrayList<>();
        for (String headerName : PRESERVED_HEADER_NAMES) {
            Header header = headers.lastHeader(headerName);
            if (header != null) {
                messageHeaders.add(MessageHeader.builder()
                        .headerName(headerName)
                        .headerValue(header.value())
                        .bufferedMessage(bufferedMessage)
                        .build());
            }
        }
        return messageHeaders;
    }

    private String getClusterName(ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord) {
        String clusterName = ClusterNameHeaderInterceptor.getClusterName(consumerRecord);
        return clusterName == null ? kafkaProperties.getDefaultClusterName() : clusterName;
    }

    Optional<SequencedMessage> findByMessageTypeAndIdempotenceId(String messageType, String idempotenceId) {
        return messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction(messageType, idempotenceId);
    }

    boolean isReleaseConditionSatisfied(SequencedMessageType sequencedMessageType, long sequenceInstanceId) {
        // Avoid querying the database if there is no release condition. In case the message does not have a release
        // condition (first message in a sequence), it should be processed immediately.
        Set<String> emptyProcessedMessageSet = Set.of();
        if (sequencedMessageType.isReleaseConditionSatisfied(emptyProcessedMessageSet)) {
            return true;
        }

        Set<String> processedMessageTypes = messageRepository.getProcessedMessageTypesInSequenceInNewTransaction(sequenceInstanceId);
        return sequencedMessageType.isReleaseConditionSatisfied(processedMessageTypes);
    }

    boolean areAllMessagesProcessed(Sequence sequence, long sequenceInstanceId) {
        Set<String> allMessageTypeQns = sequence.getMessageTypeQualifiedNames();
        Set<String> processedMessageTypeQns = messageRepository.getProcessedMessageTypesInSequenceInNewTransaction(sequenceInstanceId);
        return allMessageTypeQns.equals(processedMessageTypeQns);
    }
}
