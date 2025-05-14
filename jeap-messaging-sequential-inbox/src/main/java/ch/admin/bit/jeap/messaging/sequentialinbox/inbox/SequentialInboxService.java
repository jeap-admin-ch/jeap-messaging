package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.Sequence;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequencedMessageType;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequentialInboxConfiguration;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequenceInstance;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessage;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequencedMessageState;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxMessageHandler;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class SequentialInboxService {
    private final SequenceInstanceFactory sequenceInstanceFactory;
    private final SequencedMessageService sequencedMessageService;
    private final SequentialInboxConfiguration inboxConfiguration;
    private final Transactions tx;
    private final MessageHandlerService messageHandlerService;
    private final BufferedMessageService bufferedMessageService;

    @Value("${jeap.messaging.sequential-inbox.sequencing-start-timestamp:#{null}}")
    public LocalDateTime sequencingStartTimestamp;

    @Timed(value = "jeap.messaging.sequential-inbox.handle-message", percentiles = {0.5, 0.8, 0.95, 0.99})
    public void handleMessage(ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord,
                              SequentialInboxMessageHandler messageHandler,
                              Acknowledgment acknowledgment) {

        AvroMessage avroMessage = consumerRecord.value();
        String qualifiedSequencedMessageTypeName = inboxConfiguration.qualifiedSequencedMessageTypeName(avroMessage);
        SequencedMessageType sequencedMessageType = inboxConfiguration.requireSequencedMessageTypeByQualifiedName(qualifiedSequencedMessageTypeName);

        // Should the message be sequenced at all?
        String contextId = getContextId(avroMessage, sequencedMessageType);
        if (contextId == null) {
            log.debug("Message {} is filtered out from sequencing, handling immediately", avroMessage);
            messageHandler.invoke(consumerRecord.key(), avroMessage);
            acknowledgment.acknowledge();
            return;
        }

        // Create / get sequence instance for this context ID
        Sequence sequence = inboxConfiguration.getSequenceByQualifiedSequencedMessageTypeName(qualifiedSequencedMessageTypeName);
        log.info("Handling message {} ({}) in sequence {} with context ID {}",
                qualifiedSequencedMessageTypeName, avroMessage.getIdentity().getId(), sequence.getName(), contextId);
        long sequenceInstanceId = sequenceInstanceFactory.createOrGetSequenceInstance(sequence, contextId);

        // If the sequencing start timestamp is set and the current time is before the start timestamp, start the record mode and handle the message immediately.
        // The record activates sequencing with a delay. Until activation, the predecessor messages are recorded (Recording Mode).
        // This is needed to handle messages that need to be newly sequenced, but their predecessor was received before the introduction of the sequence.
        boolean recordingModeIsEnabled = sequencingStartTimestamp != null && LocalDateTime.now().isBefore(sequencingStartTimestamp);

        // Process the message before acquiring the lock
        handleMessage(consumerRecord, messageHandler, sequencedMessageType, sequenceInstanceId, sequence, contextId, recordingModeIsEnabled, qualifiedSequencedMessageTypeName);

        // Lock the sequence instance for update to avoid concurrent access to the inbox for the current context
        tx.runInNewTransaction(() -> {
            SequenceInstance lockedSequenceInstance = sequenceInstanceFactory.getExistingSequenceInstanceAndLockForUpdate(sequenceInstanceId);

            boolean sequenceComplete;
            if (recordingModeIsEnabled) {
                // After all messages are processed, the sequence is completed
                sequenceComplete = sequencedMessageService.areAllMessagesProcessed(sequence, sequenceInstanceId);
            } else {
                // Check for waiting messages after handling the current message
                sequenceComplete = bufferedMessageService.processBufferedMessages(lockedSequenceInstance, sequence);
            }

            // Set the sequence to complete if all messages have been processed
            if (sequenceComplete) {
                lockedSequenceInstance.close();
            }
        });

        // Acknowledge the current record
        acknowledgment.acknowledge();
    }

    private String getContextId(AvroMessage avroMessage, SequencedMessageType sequencedMessageType) {
        if (!sequencedMessageType.shouldSequenceMessage(avroMessage)) {
            return null;
        }
        return sequencedMessageType.extractContextId(avroMessage);
    }

    private void handleMessage(ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord, SequentialInboxMessageHandler messageHandler,
                               SequencedMessageType sequencedMessageType, long sequenceInstanceId, Sequence sequence, String contextId, boolean recordModeIsEnabled, String qualifiedSequencedMessageTypeName) {
        AvroMessage avroMessage = consumerRecord.value();

        Optional<SequencedMessage> existingSequencedMessage = sequencedMessageService
                .findByMessageTypeAndIdempotenceId(qualifiedSequencedMessageTypeName, avroMessage.getIdentity().getIdempotenceId());
        // Idempotence handling: Has the message already been successfully persisted or is it a new message?
        if (!isAlreadyProcessedOrWaiting(existingSequencedMessage)) {

            if (recordModeIsEnabled) {
                log.info("Recording mode active, handling message {} immediately", avroMessage);
                invokeMessageHandler(consumerRecord, messageHandler, existingSequencedMessage, sequenceInstanceId, qualifiedSequencedMessageTypeName);
                return;
            }

            // If the release condition is not satisfied, buffer the message and return
            if (!sequencedMessageService.isReleaseConditionSatisfied(sequencedMessageType, sequenceInstanceId)) {
                bufferMessage(consumerRecord, sequence, contextId, existingSequencedMessage, sequenceInstanceId, qualifiedSequencedMessageTypeName);
                return;
            }

            // Release condition is satisfied, invoke the message handler
            invokeMessageHandler(consumerRecord, messageHandler, existingSequencedMessage, sequenceInstanceId, qualifiedSequencedMessageTypeName);
        } else {
            log.info("Message {} (id={}) has already been processed with idempotence ID {}, skipping listener invocation", qualifiedSequencedMessageTypeName,
                    avroMessage.getIdentity().getId(), avroMessage.getIdentity().getIdempotenceId());
        }
    }

    private void invokeMessageHandler(ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord, SequentialInboxMessageHandler messageHandler,
                                      Optional<SequencedMessage> existingSequencedMessage, long sequenceInstanceId, String qualifiedSequencedMessageTypeName) {
        AvroMessage avroMessage = consumerRecord.value();
        try {
            log.debug("Invoking message handler for message {} (id={})", qualifiedSequencedMessageTypeName, avroMessage.getIdentity().getId());
            messageHandlerService.invokeMessageHandler(consumerRecord.key(), avroMessage, consumerRecord.topic(), messageHandler);
            sequencedMessageService.storeSequencedMessage(qualifiedSequencedMessageTypeName, existingSequencedMessage, sequenceInstanceId, SequencedMessageState.PROCESSED, consumerRecord);
        } catch (Exception ex) {
            // Exception is logged by the error service sender
            log.error("Error processing message {} (id={}), marking as failed", qualifiedSequencedMessageTypeName, avroMessage.getIdentity().getId());
            sequencedMessageService.storeSequencedMessage(qualifiedSequencedMessageTypeName, existingSequencedMessage, sequenceInstanceId, SequencedMessageState.FAILED, consumerRecord);
            throw ex; // Process record in error handler and send to the error handling service
        }
    }

    private void bufferMessage(ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord, Sequence sequence, String contextId, Optional<SequencedMessage> existingSequencedMessage, long sequenceInstanceId, String qualifiedSequencedMessageTypeName) {
        log.info("Buffering message {} in sequence {} with context ID {}", qualifiedSequencedMessageTypeName, sequence.getName(), contextId);
        sequencedMessageService.storeSequencedMessage(qualifiedSequencedMessageTypeName, existingSequencedMessage, sequenceInstanceId, SequencedMessageState.WAITING, consumerRecord);
    }

    private static boolean isAlreadyProcessedOrWaiting(Optional<SequencedMessage> existingSequencedMessage) {
        return existingSequencedMessage.isPresent() &&
                SequencedMessageState.waitingOrProcessed(existingSequencedMessage.get().getState());
    }
}
