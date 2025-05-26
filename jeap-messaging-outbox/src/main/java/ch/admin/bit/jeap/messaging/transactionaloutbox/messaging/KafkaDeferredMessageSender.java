package ch.admin.bit.jeap.messaging.transactionaloutbox.messaging;

import ch.admin.bit.jeap.messaging.kafka.signature.SignatureService;
import ch.admin.bit.jeap.messaging.kafka.tracing.TracingKafkaTemplateFactory;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.*;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxMetrics.MESSAGES_TRANSMIT_TIMER;
import static ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxMetrics.MESSAGE_DELIVERY_TYPE_IMMEDIATE;
import static ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxMetrics.MESSAGE_DELIVERY_TYPE_SCHEDULED;
import static ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxMetrics.MESSAGE_DELIVERY_TYPE_TAG;

/**
 * The transactional outbox uses its own KafkaTemplate instances to send Kafka messages. It does not use the standard one provided by the
 * jEAP messaging library. The standard template is configured to serialize only Avro types using a KafkaAvroSerializer implementation.
 * In addition, there are interceptors configured for the standard template that require jEAP messaging MessageType implementations.
 * <p>
 * The transactional outbox needs to send messages that have already been serialized. Therefore we instantiate a separate
 * KafkaTemplate that is based on a ProducerFactory with the same configuration as the standard producer factory but with
 * byte array serializers and without the interceptor configuration.
 * <p>
 * In addition, we instantiate two different KafkaTemplate instances, one to be used for sending messages synchronously immediately
 * after the transaction committed and one to be used for sending messages asynchronously by a scheduled message relaying process. We
 * need to use two different templates in order to be able to specify different timeout configurations for the two different
 * sending purposes. The reason fo this is that the timeout configurations are part of the Kafka producer configuration and cannot
 * be set per message sent.
 * <p>
 * By setting the timeouts in the Kafka producer instead of just calling get() with a timeout on the send future, we're giving Kafka
 * the possibility to not transmit timed-out messages. This would not be the case when just using the timeout on the send future
 * as Kafka does (according to the source code) not seem to do anything when cancel() is called on the send future. Setting the
 * timeouts in the Kafka producer also allows to take into account the producer blocking timeout 'max.block.ms' which would not
 * be accounted for by the timeout on the send future.
 */
@Slf4j
class KafkaDeferredMessageSender implements DeferredMessageSender {

    private final KafkaTemplate<byte[], byte[]> kafkaTemplateImmediateSending;
    private final KafkaTemplate<byte[], byte[]> kafkaTemplateScheduledSending;
    private final TransactionalOutboxConfiguration config;
    private final OutboxTracing outboxTracing;
    private final Optional<TracingKafkaTemplateFactory> tracingKafkaTemplateFactory;
    private final Optional<OutboxMetrics> outboxMetrics; // Collection of outbox metrics depends on a metrics setup being provided.
    private final Optional<SignatureService> signatureService;
    private final String bootstrapServers;

    KafkaDeferredMessageSender(ProducerFactory<byte[], byte[]> producerFactory,
                               TransactionalOutboxConfiguration config,
                               OutboxTracing outboxTracing,
                               Optional<TracingKafkaTemplateFactory> tracingKafkaTemplateFactory,
                               Optional<OutboxMetrics> outboxMetrics,
                               Optional<SignatureService> signatureService) {
        this.outboxTracing = outboxTracing;
        this.tracingKafkaTemplateFactory = tracingKafkaTemplateFactory;
        this.outboxMetrics = outboxMetrics;
        this.signatureService = signatureService;
        this.bootstrapServers = (String) producerFactory.getConfigurationProperties().get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG);
        kafkaTemplateImmediateSending = createKafkaTemplate(producerFactory, Map.of(
                ProducerConfig.MAX_BLOCK_MS_CONFIG, config.getMessageSendImmediatelyMaxBlockTime().toMillis(),
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, getIntMillis(config.getMessageSendImmediatelyTimeout()),
                ProducerConfig.LINGER_MS_CONFIG, 0,
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, getIntMillis(config.getMessageSendImmediatelyTimeout()))
        );
        kafkaTemplateScheduledSending = createKafkaTemplate(producerFactory, Map.of(
                ProducerConfig.MAX_BLOCK_MS_CONFIG, config.getMessageSendScheduledMaxBlockTime().toMillis(),
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, getIntMillis(config.getMessageSendScheduledTimeout()),
                ProducerConfig.LINGER_MS_CONFIG, 0,
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, getIntMillis(config.getMessageSendScheduledTimeout()))
        );
        this.config = config;
    }

    private int getIntMillis(Duration duration) {
        return (int) duration.toMillis();
    }

    private KafkaTemplate<byte[], byte[]> createKafkaTemplate(ProducerFactory<byte[], byte[]> producerFactory, Map<String, Object> additionalConfig) {
        var producerFactoryConfig = new HashMap<>(producerFactory.getConfigurationProperties());
        producerFactoryConfig.remove(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG);
        producerFactoryConfig.putAll(additionalConfig);

        if (tracingKafkaTemplateFactory.isPresent()) {
            return tracingKafkaTemplateFactory.get().createKafkaTemplate(producerFactoryConfig, byteArraySerializer(), byteArraySerializer());
        } else {
            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerFactoryConfig, this::byteArraySerializer, this::byteArraySerializer));
        }
    }

    private Serializer<byte[]> byteArraySerializer() {
        return new ByteArraySerializer();
    }

    @Timed(value = MESSAGES_TRANSMIT_TIMER, extraTags = {MESSAGE_DELIVERY_TYPE_TAG, MESSAGE_DELIVERY_TYPE_IMMEDIATE},
            description = "Outbox message transmits for immediate delivery.")
    @Override
    public void sendAsImmediate(DeferredMessage deferredMessage) {
        send(deferredMessage, config.getMessageSendImmediatelyTimeout(), kafkaTemplateImmediateSending);
    }

    @Timed(value = MESSAGES_TRANSMIT_TIMER, extraTags = {MESSAGE_DELIVERY_TYPE_TAG, MESSAGE_DELIVERY_TYPE_SCHEDULED},
            description = "Outbox message transmits for scheduled delivery.")
    @Override
    public void sendAsScheduled(DeferredMessage deferredMessage) {
        send(deferredMessage, config.getMessageSendScheduledTimeout(), kafkaTemplateScheduledSending);
    }

    private void send(DeferredMessage deferredMessage, Duration sendTimeout, KafkaTemplate<byte[], byte[]> kafkaTemplate) {
        final byte[] key = deferredMessage.getKey();
        final byte[] message = deferredMessage.getMessage();
        final String topic = deferredMessage.getTopic();
        // "+ 500" -> grant the Kafka producer some time to raise its own expected specific timeout exception during the send future execution.
        final long sendFutureTimeoutMillis = sendTimeout.toMillis() + 500;
        DeferredMessageLogArgument deferredMessageLogArgument = DeferredMessageLogArgument.from(deferredMessage);

        outboxTracing.updateCurrentTraceContext(deferredMessage.getTraceContext());


        ProducerRecord<byte[], byte[]> producerRecord =  new ProducerRecord<>(topic, key, message);
        injectSignatureHeadersIfNeeded(producerRecord, message, key);

        try {
            log.debug("Sending message {} to Kafka with a timeout of {} millis.", deferredMessageLogArgument, sendFutureTimeoutMillis);
            kafkaTemplate.send(producerRecord).get(sendFutureTimeoutMillis, TimeUnit.MILLISECONDS);

            outboxMetrics.ifPresent(metrics ->
                    metrics.countMessagingSend(bootstrapServers, topic, deferredMessage.getMessageTypeName(), deferredMessage.getMessageTypeVersion()));

            log.debug("Successfully sent {}.", deferredMessageLogArgument);
        } catch (InterruptedException ie) {
            log.error("Failed sending {}.", deferredMessageLogArgument);
            Thread.currentThread().interrupt();
            convertException(deferredMessage, ie);
        } catch (Exception e) {
            log.error("Failed sending {}.", deferredMessageLogArgument);
            convertException(deferredMessage, e);
        }
    }

    private void injectSignatureHeadersIfNeeded(ProducerRecord<byte[], byte[]> producerRecord, byte[] message, byte[] key) {
        signatureService.ifPresent(service -> {
            Headers headers = producerRecord.headers();
            service.injectSignature(headers, message, false);
            if (key != null) {
                service.injectSignature(headers, key, true);
            }
        });
    }

    private void convertException(DeferredMessage deferredMessage, Exception e) {
        if (e instanceof KafkaException) {
            Throwable mostSpecificCause = ((KafkaException) e).getMostSpecificCause();
            if (mostSpecificCause instanceof InvalidTopicException) {
                throw DeferredMessageSendException.invalidTopicException(deferredMessage, e);
            } else if (mostSpecificCause instanceof TopicAuthorizationException) {
                throw DeferredMessageSendException.topicAuthorizationException(deferredMessage, e);
            } else if (mostSpecificCause instanceof RecordTooLargeException) {
                throw DeferredMessageSendException.messageTooLargeException(deferredMessage, e);
            }
        }

        throw DeferredMessageSendException.generalSendException(deferredMessage, e);
    }

}
