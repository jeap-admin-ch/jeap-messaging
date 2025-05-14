package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.avro.MessageVersionAccessor;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.interceptor.Callbacks;
import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.messaging.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * The transactional outbox allows to modify persistent data and send messages within one transaction. It does so by
 * persisting the messages together with the data in the same database and within the same database transaction.
 * The persisted messages are only sent after the transaction committed. If the transaction is rolled back, the messages
 * won't be sent.
 * <p>
 * This outbox implementation supports two different ways for effectively sending the messages: send them immediately
 * after the transaction committed in the transaction's thread or send them in a separate relay process that is scheduled
 * to run periodically and poll the database for persisted messages to be sent. If there occurs a problem during the
 * immediate sending of messages after the transaction committed, the immediate sending will be aborted and the messages
 * will instead be sent later (after a certain delay) by the separate scheduled relay process (aka the second way).
 * <p>
 * For most use cases 'sending immediately' should be the appropriate way to send messages using the transactional outbox.
 * <p>
 * Sending immediately (send() methods) minimizes the latency and allows scaling up the sending of messages by scaling
 * up the number of application instances. Sending scheduled (sendScheduled() methods) uses slightly less time in the
 * thread that puts the messages in the outbox because the sending is done by another thread in the background or even by
 * another application instance. On the downside sending scheduled in the background adds latency and is serial, i.e.
 * there is no scaling up the effective sending by scaling up the number of application instances. However, if one
 * application instance dies another one will pick up the sending in the background from the died instance.
 * <p>
 * Sending scheduled in the background can be disabled by setting jeap.messaging.transactional-outbox.scheduled-relay-enabled
 * to false. This could be used to free application instances that are serving customer request from effectively sending messages
 * and instantiate separate message relay service instances for effectively sending the messages.
 **/
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Slf4j
@RequiredArgsConstructor
public class TransactionalOutbox {

    private final String clusterName;
    private final MessageSerializer serializer;
    private final DeferredMessageRepository deferredMessageRepository;
    private final FailedMessageRepository failedMessageRepository;
    private final AfterCommitMessageSender afterCommitMessageSender;
    private final ContractsValidator contractsValidator;
    private final Optional<OutboxMetrics> outboxMetrics;  // Collection of outbox metrics depends on a metrics setup being provided.
    private final OutboxTracing outboxTracing;
    private final List<JeapKafkaMessageCallback> callbacks;

    /**
     * Send the given message to the given topic. Sending will happen immediately after the surrounding transaction got committed.
     * If the immediate sending fails the message will be picked up again by the scheduled message relay process which will send it later
     * after some delay.
     *
     * @param message The message
     * @param topic   The topic
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void sendMessage(Message message, String topic) {
        sendMessage(message, null, topic);
    }

    /**
     * Send the given message to the given topic. Sending will happen some time later when the scheduled message relay process
     * will pick the message up and send it.
     *
     * @param message The message
     * @param topic   The topic
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void sendMessageScheduled(Message message, String topic) {
        sendMessageScheduled(message, null, topic);
    }

    /**
     * Send the given message with the given key to the given topic. Sending will happen immediately after the surrounding transaction got committed.
     * If the immediate sending fails the message will be picked up again by the scheduled message relay process which will send it later
     * after some delay.
     *
     * @param message The message
     * @param key     The key
     * @param topic   The topic
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void sendMessage(Message message, Object key, String topic) {
        sendMessage(message, key, topic, true);
    }

    /**
     * Send the given message with the given key to the given topic. Sending will happen some time later when the scheduled message relay process
     * will pick the message up and send it.
     *
     * @param message The message
     * @param key     The key
     * @param topic   The topic
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void sendMessageScheduled(Message message, Object key, String topic) {
        sendMessage(message, key, topic, false);
    }

    /**
     * Count the number of messages having state 'failed'.
     *
     * @param resend Count only messages that have been marked for a resend (<code>true</code>) or not (<code>false</code>).
     * @return The number of messages counted.
     */
    public int countFailedMessages(boolean resend) {
        return failedMessageRepository.countFailedMessages(resend);
    }

    /**
     * Count the number of messages having state 'failed' that failed during the specified interval.
     *
     * @param failedStartingFrom Only messages which have state 'failed' and failed starting from the given instant will be found.
     * @param failedBefore       Count only messages which have state 'failed' and failed before the given instant will be found.
     * @param resend             Only messages that have been marked for a resend (<code>true</code>) or not (<code>false</code>).
     * @return The number of messages counted.
     */
    public int countFailedMessages(ZonedDateTime failedStartingFrom, ZonedDateTime failedBefore, boolean resend) {
        return failedMessageRepository.countFailedMessages(failedStartingFrom, failedBefore, resend);
    }

    /**
     * Find messages that have state 'failed' and have an id greater than the given id and have failed before the given instant.
     *
     * @param failedStartingFrom   Only messages which have state 'failed' and failed starting from the given instant will be found.
     * @param failedBefore         Only messages which have state 'failed' and failed before the given instant will be found.
     * @param resend               Find only messages that have been marked for a resend (<code>true</code>) or not (<code>false</code>).
     * @param maxNumMessagesToFind The maximum number of failed messages that will be found.
     * @return The failed messages found ordered by their ids (ascending).
     */
    public List<FailedMessage> findFailedMessages(ZonedDateTime failedStartingFrom, ZonedDateTime failedBefore, boolean resend, int maxNumMessagesToFind) {
        return failedMessageRepository.findFailedMessages(failedStartingFrom, failedBefore, resend, maxNumMessagesToFind);
    }

    /**
     * Find messages that have state 'failed' and have an id greater than the given id and have failed before the given instant.
     *
     * @param afterId              Only messages which have state 'failed' and Ids greater than the given id will be found.
     * @param failedBefore         Only messages which have state 'failed' and failed before the given instant will be found.
     * @param resend               Find only messages that have been marked for a resend (<code>true</code>) or not (<code>false</code>).
     * @param maxNumMessagesToFind The maximum number of failed messages that will be found.
     * @return The failed messages found ordered by their ids (ascending).
     */
    public List<FailedMessage> findFailedMessages(long afterId, ZonedDateTime failedBefore, boolean resend, int maxNumMessagesToFind) {
        return failedMessageRepository.findFailedMessages(afterId, failedBefore, resend, maxNumMessagesToFind);
    }

    /**
     * Make the given outbox message available again to the message relay process for delivery.
     *
     * @param id The id of the outbox message to make available again to the message relay process for delivery.
     * @throws TransactionalOutboxException if the message could not be found.
     */
    public void resendMessageScheduled(long id) {
        deferredMessageRepository.markForResend(id, true);
    }

    private void sendMessage(Message message, Object key, String topic, boolean sendImmediately) {
        ensurePublisherContract(message, topic);
        byte[] serializedMessage = serializer.serializeMessage(message, topic);
        byte[] serializedKey = Optional.ofNullable(key)
                .map(nonNullKey -> serializer.serializeKey(nonNullKey, topic))
                .orElse(null);
        DeferredMessage newDeferredMessage = DeferredMessage.builder()
                .message(serializedMessage)
                .key(serializedKey)
                .clusterName(clusterName)
                .topic(topic)
                .messageId(message.getIdentity().getId())
                .messageIdempotenceId(message.getIdentity().getIdempotenceId())
                .messageTypeName(message.getType().getName())
                .messageTypeVersion(MessageVersionAccessor.getGeneratedVersion(message.getClass()))
                .sendImmediately(sendImmediately)
                .traceContext(outboxTracing.retrieveCurrentTraceContext())
                .build();
        DeferredMessage persistedDeferredMessage = deferredMessageRepository.save(newDeferredMessage);
        log.debug("Persisted {}.", DeferredMessageLogArgument.from(persistedDeferredMessage));
        if (sendImmediately) {
            afterCommitMessageSender.sendImmediatelyAfterTransactionCommit(persistedDeferredMessage);
        }
        outboxMetrics.ifPresent(metrics -> metrics.countTransactionalSend(sendImmediately));
        invokeOnSendCallbacks(message, topic);
    }

    private void invokeOnSendCallbacks(Message msg, String topic) {
        callbacks.forEach(callback -> Callbacks.invokeCallback(msg, topic, callback::onSend));
    }

    private void ensurePublisherContract(Message message, String topic) {
        try {
            contractsValidator.ensurePublisherContract(message.getType(), topic);
        } catch (Exception e) {
            log.error("Contract validation for message with {}, {} and {} failed.", kv("messageId", message.getIdentity().getId()),
                    kv("messageIdempotenceId", message.getIdentity().getIdempotenceId()), kv("messageType", message.getType().getName()), e);
            throw TransactionalOutboxException.contractValidationFailed(message, e);
        }
    }

}
