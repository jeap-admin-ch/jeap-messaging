package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.kafka.errorhandling.ErrorServiceSender;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.Sequence;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequencedMessageType;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequentialInboxConfiguration;
import ch.admin.bit.jeap.messaging.sequentialinbox.inbox.BufferedMessageTracing.TraceContextRestorer;
import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.MessageRepository;
import ch.admin.bit.jeap.messaging.sequentialinbox.metrics.SequentialInboxMetricsCollector;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.BufferedMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequenceInstance;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessageState;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
class BufferedMessageService {

    private final ErrorServiceSender errorServiceSender;
    private final SequentialInboxDeserializer inboxDeserializer;
    private final MessageHandlerService messageHandlerService;
    private final MessageRepository messageRepository;
    private final SequentialInboxConfiguration sequentialInboxConfiguration;
    private final BufferedMessageTracing bufferedMessageTracing;
    private final SequentialInboxMetricsCollector metricsCollector;

    /**
     * @return true if sequence is complete, false otherwise
     */
    boolean processBufferedMessages(SequenceInstance sequenceInstance, Sequence sequence) {
        List<SequencedMessage> waitingAndProcessedMessages = messageRepository.getWaitingAndProcessedMessagesInNewTransaction(sequenceInstance.getId());
        List<SequencedMessage> waitingMessages = waitingMessagesInModifiableList(waitingAndProcessedMessages);
        Set<String> processedMessageTypes = processedMessageTypes(waitingAndProcessedMessages);

        boolean waitingMessageProcessed;
        do {
            Optional<SequencedMessage> nextWaitingMessageReadyToBeProcessed = waitingMessages.stream()
                    .filter(sequencedMessage -> sequencedMessageType(sequencedMessage).isReleaseConditionSatisfied(processedMessageTypes))
                    .findFirst();

            if (nextWaitingMessageReadyToBeProcessed.isPresent()) {
                log.debug("Next waiting message ready to be processed: {}", nextWaitingMessageReadyToBeProcessed);
                SequencedMessage sequencedMessage = nextWaitingMessageReadyToBeProcessed.get();
                waitingMessages.remove(sequencedMessage);
                waitingMessageProcessed = true;

                boolean success = handleBufferedMessage(sequencedMessage);
                if (success) {
                    processedMessageTypes.add(sequencedMessage.getMessageType());
                }
            } else {
                log.debug("No waiting message ready to be processed in sequence {}", sequenceInstance);
                waitingMessageProcessed = false;
            }
        } while (waitingMessageProcessed);

        return sequence.isComplete(processedMessageTypes);
    }

    private SequencedMessageType sequencedMessageType(SequencedMessage sequencedMessage) {
        return sequentialInboxConfiguration.requireSequencedMessageTypeByQualifiedName(sequencedMessage.getMessageType());
    }

    /**
     * @return true if the message was successfully handled, false otherwise
     */
    private boolean handleBufferedMessage(SequencedMessage sequencedMessage) {
        try (TraceContextRestorer ignored = bufferedMessageTracing.updateCurrentTraceContext(sequencedMessage.getTraceContext())) {
            Optional<DeserializedMessage> deserializedMessage = getDeserializedMessage(sequencedMessage);
            if (deserializedMessage.isEmpty()) {
                log.debug("Deserialization failed for message {}", sequencedMessage);
                return false;
            }

            try {
                log.debug("Processing buffered message {}", sequencedMessage);
                recordWaitingMessageCompletedTimer(sequencedMessage);
                messageHandlerService.handle(deserializedMessage.get());
                messageRepository.setMessageStateInNewTransaction(sequencedMessage, SequencedMessageState.PROCESSED);
                log.debug("Processed buffered message {}", sequencedMessage);
            } catch (Exception ex) {
                FailedConsumerRecord failedConsumerRecord = FailedConsumerRecord.of(sequencedMessage, deserializedMessage.get().key(), deserializedMessage.get().message());
                sendMessageToErrorHandlerAndMarkFailed(sequencedMessage, ex, failedConsumerRecord);
                return false;
            }
        }

        return true;
    }

    private void recordWaitingMessageCompletedTimer(SequencedMessage sequencedMessage) {
        // Record only the state change from WAITING to PROCESSED, ignore the case when the message is retried from the FAILED state
        if (sequencedMessage.getState() == SequencedMessageState.WAITING) {
            Duration waitDuration = Duration.between(sequencedMessage.getCreatedAt(), ZonedDateTime.now());
            metricsCollector.onWaitingMessageCompleted(sequencedMessage.getMessageType(), waitDuration);
        }
    }

    private Optional<DeserializedMessage> getDeserializedMessage(SequencedMessage sequencedMessage) {
        BufferedMessage bufferedMessage = messageRepository.getBufferedMessageInNewTransaction(sequencedMessage);
        DeserializedMessage deserializedMessage;
        try {
            deserializedMessage = inboxDeserializer.deserialize(sequencedMessage, bufferedMessage);

        } catch (Exception ex) {
            // Exception while deserializing - pass raw serialized bytes to the MessageProcessingFailedEventBuilder
            FailedConsumerRecord failedConsumerRecord = FailedConsumerRecord.of(sequencedMessage, bufferedMessage);
            sendMessageToErrorHandlerAndMarkFailed(sequencedMessage, ex, failedConsumerRecord);
            return Optional.empty();
        }
        if (deserializedMessage.deserializationFailed()) {
            sendMessageToErrorHandlerAndMarkFailed(sequencedMessage,
                    SequentialInboxException.deserializationFailed(sequencedMessage),
                    FailedConsumerRecord.of(sequencedMessage, deserializedMessage));
            return Optional.empty();
        }
        return Optional.of(deserializedMessage);
    }

    private void sendMessageToErrorHandlerAndMarkFailed(SequencedMessage sequencedMessage, Exception ex, FailedConsumerRecord record) {
        errorServiceSender.accept(record, ex);
        messageRepository.setMessageStateInNewTransaction(sequencedMessage, SequencedMessageState.FAILED);
    }

    private static List<SequencedMessage> waitingMessagesInModifiableList(List<SequencedMessage> waitingAndProcessedMessages) {
        return waitingAndProcessedMessages.stream()
                .filter(sequencedMessage -> sequencedMessage.getState() == SequencedMessageState.WAITING)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static Set<String> processedMessageTypes(List<SequencedMessage> waitingAndProcessedMessages) {
        return waitingAndProcessedMessages.stream()
                .filter(sequencedMessage -> sequencedMessage.getState() == SequencedMessageState.PROCESSED)
                .map(SequencedMessage::getMessageType)
                .collect(Collectors.toSet());
    }
}
