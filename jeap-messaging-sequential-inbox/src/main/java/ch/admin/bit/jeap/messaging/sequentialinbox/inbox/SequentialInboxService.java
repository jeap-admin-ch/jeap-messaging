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

    @Timed(value = "jeap.sequentialinbox.handlemessage", histogram = true, percentiles = {0.5, 0.95, 0.99})
    public void handleMessage(ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord,
                              SequentialInboxMessageHandler messageHandler,
                              Acknowledgment acknowledgment) {

        AvroMessage avroMessage = consumerRecord.value();
        String messageTypeName = avroMessage.getType().getName();
        SequencedMessageType sequencedMessageType = inboxConfiguration.requireSequencedMessageTypeByName(messageTypeName);

        // Should the message be sequenced at all?
        String contextId = getContextId(avroMessage, sequencedMessageType);
        if (contextId == null) {
            log.debug("Message {} is filtered out from sequencing, handling immediately", avroMessage);
            messageHandler.invoke(consumerRecord.key(), avroMessage);
            acknowledgment.acknowledge();
            return;
        }

        // Create / get sequence instance for this context ID
        Sequence sequence = inboxConfiguration.getSequenceByMessageTypeName(messageTypeName);
        log.info("Handling message {} ({}) in sequence {} with context ID {}",
                avroMessage.getType().getName(), avroMessage.getIdentity().getId(), sequence.getName(), contextId);
        long sequenceInstanceId = sequenceInstanceFactory.createOrGetSequenceInstance(sequence, contextId);

        // Lock the sequence instance for update to avoid concurrent access to the inbox for the current context
        tx.runInNewTransaction(() -> {
            SequenceInstance sequenceInstance = sequenceInstanceFactory.getExistingSequenceInstanceAndLockForUpdate(sequenceInstanceId);

            // Suspend the transaction to make sure that the sequence instance locking transaction does not leak into
            // the message handling code. The locking transaction controls access to the inbox for the current context,
            // while the message handling transactions control the processing of individual messages.
            boolean sequenceComplete = tx.callInSuspendedTransaction(() ->
                    handleMessage(consumerRecord, messageHandler, sequencedMessageType, sequenceInstance, sequence, contextId)
            );

            // Set the sequence to complete if all messages have been processed
            if (sequenceComplete) {
                sequenceInstance.close();
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

    private boolean handleMessage(ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord, SequentialInboxMessageHandler messageHandler,
                                  SequencedMessageType sequencedMessageType, SequenceInstance sequenceInstance, Sequence sequence, String contextId) {
        AvroMessage avroMessage = consumerRecord.value();
        String messageTypeName = avroMessage.getType().getName();

        Optional<SequencedMessage> existingSequencedMessage = sequencedMessageService
                .findByMessageTypeAndIdempotenceId(messageTypeName, avroMessage.getIdentity().getIdempotenceId());
        // Idempotence handling: Has the message already been successfully persisted or is it a new message?
        if (!isAlreadyProcessedOrWaiting(existingSequencedMessage)) {

            // If the sequencing start timestamp is set and the current time is before the start timestamp, start the record mode and handle the message immediately.
            // The record activates sequencing with a delay. Until activation, the predecessor messages are recorded (Recording Mode).
            // This is needed to handle messages that need to be newly sequenced, but their predecessor was received before the introduction of the sequence.
            log.info("Sequencing start timestamp: {}", sequencingStartTimestamp);
            if (sequencingStartTimestamp != null && LocalDateTime.now().isBefore(sequencingStartTimestamp)) {
                log.info("Recording mode active, handling message {} immediately", avroMessage);
                invokeMessageHandler(consumerRecord, messageHandler, existingSequencedMessage, sequenceInstance);
                // After all messages are processed, the sequence is completed
                return sequencedMessageService.areAllMessagesProcessed(sequence, sequenceInstance);
            }

            // If the release condition is not satisfied, buffer the message and return
            if (!sequencedMessageService.isReleaseConditionSatisfied(sequencedMessageType, sequenceInstance)) {
                bufferMessage(consumerRecord, avroMessage, sequence, contextId, existingSequencedMessage, sequenceInstance);
                return false;
            }

            // Release condition is satisfied, invoke the message handler
            invokeMessageHandler(consumerRecord, messageHandler, existingSequencedMessage, sequenceInstance);
        } else {
            log.info("Message {} (id={}) has already been processed with idempotence ID {}, skipping listener invocation", messageTypeName,
                    avroMessage.getIdentity().getId(), avroMessage.getIdentity().getIdempotenceId());
        }

        // Handle buffered messages with satisfied release conditions
        return bufferedMessageService.processBufferedMessages(sequenceInstance, sequence);
    }

    private void invokeMessageHandler(ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord, SequentialInboxMessageHandler messageHandler,
                                      Optional<SequencedMessage> existingSequencedMessage, SequenceInstance sequenceInstance) {
        AvroMessage avroMessage = consumerRecord.value();
        String messageTypeName = avroMessage.getType().getName();
        try {
            log.debug("Invoking message handler for message {} (id={})", messageTypeName, avroMessage.getIdentity().getId());
            messageHandlerService.invokeMessageHandler(consumerRecord.key(), avroMessage, messageHandler);
            sequencedMessageService.storeSequencedMessage(existingSequencedMessage, sequenceInstance, SequencedMessageState.PROCESSED, consumerRecord);
        } catch (Exception ex) {
            // Exception is logged by the error service sender
            log.error("Error processing message {} (id={}), marking as failed", messageTypeName, avroMessage.getIdentity().getId());
            sequencedMessageService.storeSequencedMessage(existingSequencedMessage, sequenceInstance, SequencedMessageState.FAILED, consumerRecord);
            throw ex; // Process record in error handler and send to the error handling service
        }
    }

    private void bufferMessage(ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord, AvroMessage avroMessage, Sequence sequence, String contextId, Optional<SequencedMessage> existingSequencedMessage, SequenceInstance sequenceInstance) {
        log.info("Buffering message {} in sequence {} with context ID {}", avroMessage.getType().getName(), sequence.getName(), contextId);
        sequencedMessageService.storeSequencedMessage(existingSequencedMessage, sequenceInstance, SequencedMessageState.WAITING, consumerRecord);
    }

    private static boolean isAlreadyProcessedOrWaiting(Optional<SequencedMessage> existingSequencedMessage) {
        return existingSequencedMessage.isPresent() &&
                SequencedMessageState.waitingOrProcessed(existingSequencedMessage.get().getState());
    }
}
