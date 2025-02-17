package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEventBuilder;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedMessageKey;
import ch.admin.bit.jeap.messaging.kafka.crypto.JeapKafkaAvroSerdeCryptoConfig;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureHeaders;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.messaging.kafka.tracing.TracerBridge;
import ch.admin.bit.jeap.messaging.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

import java.util.concurrent.CompletableFuture;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

/**
 * A simple handler that can be used together with {@link org.springframework.kafka.listener.DefaultErrorHandler}.
 * <p>
 * As you can read multiple events from Kafka at once, it make sense to store the offsets for the last successfully
 * handled event. This will be done by {@link org.springframework.kafka.listener.DefaultErrorHandler}.
 * {@link org.springframework.kafka.listener.DefaultErrorHandler} could also retry handling an event, however we are not using this feature (retry is done with error handling service).
 * Afterwards {@link org.springframework.kafka.listener.DefaultErrorHandler} will call this class with the failed event who will send it to the
 * error handling service. If this call does not throw an exception (or terminate the application context) the
 * {@link KafkaMessageListenerContainer} will then acknowledge the event in doInvokeRecordListener
 * as it has been handled by this class already
 */
@Slf4j
public class ErrorServiceSender implements ConsumerRecordRecoverer {
    private final BeanFactory beanFactory;
    private final KafkaProperties kafkaProperties;
    private final ErrorServiceFailedHandler errorServiceFailedHandler;
    private final BackOff backOff;
    private final TracerBridge tracerBridge;
    private final JeapKafkaBeanNames jeapKafkaBeanNames;
    private final StackTraceHasher stackTraceHasher;

    public ErrorServiceSender(BeanFactory beanFactory, KafkaProperties kafkaProperties,
                              ErrorServiceFailedHandler errorServiceFailedHandler, BackOff backOff,
                              TracerBridge tracerBridge, StackTraceHasher stackTraceHasher) {
        this.beanFactory = beanFactory;
        this.kafkaProperties = kafkaProperties;
        this.errorServiceFailedHandler = errorServiceFailedHandler;
        this.backOff = backOff;
        this.tracerBridge = tracerBridge;
        this.jeapKafkaBeanNames = new JeapKafkaBeanNames(kafkaProperties.getDefaultClusterName());
        this.stackTraceHasher = stackTraceHasher;
    }

    private static MessageHandlerExceptionInformation createSerializationExceptionInformation(ErrorSerializedMessageHolder errorSerializedMessageHolder, String errorMessage) {
        Throwable cause = new Exception(errorMessage, errorSerializedMessageHolder.getCause());
        return MessageHandlerException.builder()
                .errorCode(MessageHandlerExceptionInformation.StandardErrorCodes.DESERIALIZATION_FAILED.name())
                .temporality(MessageHandlerExceptionInformation.Temporality.PERMANENT)
                .cause(cause)
                .build();
    }

    @Override
    public void accept(ConsumerRecord<?, ?> consumerRecord, Exception exception) {
        // Spring Cloud Sleuth already closed the span in which the exception occurred and does not activate a new tracing
        // context for handling the exception. Therefore, we establish this context ourselves here by creating a child
        // span in the failed message's original trace context and by executing the error handling within this span.
        restoreOriginalTracingHeaders(consumerRecord);
        try (TracerBridge.TracerBridgeElement ignored = getSpan(consumerRecord)) {
            createAndSendMessageProcessingFailedEvent(consumerRecord, exception);
        }
    }

    private void restoreOriginalTracingHeaders(ConsumerRecord<?, ?> record) {
        if (tracerBridge != null) {
            tracerBridge.restoreOriginalTraceContext(record);
        }
    }

    private TracerBridge.TracerBridgeElement getSpan(ConsumerRecord<?, ?> record) {
        if (tracerBridge != null) {
            return tracerBridge.getSpan(record);
        } else {
            return () -> {
            };
        }
    }

    private void createAndSendMessageProcessingFailedEvent(ConsumerRecord<?, ?> consumerRecord, Exception exception) {
        MessageProcessingFailedEvent event = createMessageProcessingFailedEvent(consumerRecord, exception);
        BackOffExecution backOffExecution = backOff.start();
        MessageProcessingFailedMessageKey key =
                createMessageProcessingFailedMessageKey(consumerRecord, event.getIdentity().getIdempotenceId());

        String clusterName = getErrorHandlingTopicClusterName(consumerRecord);
        sendMessageProcessingFailedEventWithRetry(key, event, backOffExecution, clusterName);
    }

    private String getErrorHandlingTopicClusterName(ConsumerRecord<?, ?> consumerRecord) {
        if (kafkaProperties.hasDefaultProducerClusterOverride()) {
            return kafkaProperties.getDefaultProducerClusterOverride();
        }

        String clusterName = ClusterNameHeaderInterceptor.getClusterName(consumerRecord);
        if (clusterName == null) {
            clusterName = kafkaProperties.getDefaultClusterName();
        }
        return clusterName;
    }

    private MessageProcessingFailedEvent createMessageProcessingFailedEvent(ConsumerRecord<?, ?> consumerRecord, Exception exception) {
        MessageHandlerExceptionInformation exceptionInformation = getExceptionInformation(consumerRecord, exception);
        MessageProcessingFailedEvent event = MessageProcessingFailedEventBuilder.create()
                .systemName(kafkaProperties.getSystemName())
                .serviceName(kafkaProperties.getServiceName())
                .originalMessage(consumerRecord,
                        JeapKafkaAvroSerdeCryptoConfig.ENCRYPTED_VALUE_HEADER_NAME,
                        SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY,
                        SignatureHeaders.SIGNATURE_PAYLOAD_HEADER_KEY,
                        SignatureHeaders.SIGNATURE_KEY_HEADER_KEY
                )
                .eventHandleException(exceptionInformation)
                .stackTraceMaxLength(kafkaProperties.getErrorEventStackTraceMaxLength())
                .stackTraceHash(getStackTraceHash(exception))
                .build();
        logError(consumerRecord.value(), exception);
        return event;
    }

    private String getStackTraceHash(Exception exception) {
        if (kafkaProperties.isErrorStackTraceHashEnabled()) {
            return stackTraceHasher.hash(exception);
        } else {
            return null;
        }
    }

    private MessageProcessingFailedMessageKey createMessageProcessingFailedMessageKey(ConsumerRecord<?, ?> originalMessage, String fallbackKeyValue) {
        String key;
        try {
            Message message = (Message) originalMessage.value();
            key = message.getIdentity().getId();
        } catch (Exception ex) {
            // The error service sender must be as resilient as possible against any kind of issue causing the message
            // failed event to be produced (i.e. invalid original message, ...).
            // For resilience reasons, if the ID of the causing event cannot be determined, the idempotence ID of the
            // error event is used as the key. That will still help the jeap-error-handling service to consume the
            // message processing failed events in an ordered way avoiding idempotence check race conditions.
            key = fallbackKeyValue;
        }
        return MessageProcessingFailedMessageKey.newBuilder()
                .setKey(key)
                .build();
    }

    private void sendMessageProcessingFailedEventWithRetry(MessageProcessingFailedMessageKey key,
                                                           MessageProcessingFailedEvent event,
                                                           BackOffExecution backOffExecution,
                                                           String clusterName) {
        int attempt = 1;
        do {
            try {
                KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate = getKafkaTemplate(clusterName);
                CompletableFuture<? extends SendResult<AvroMessageKey, AvroMessage>> future =
                        kafkaTemplate.send(kafkaProperties.getErrorTopicName(clusterName), key, event);
                future.get();
                log.debug("Successfully sent error event using cluster {}", clusterName);
                return;
            } catch (InterruptedException e) {
                //In case of an interrupt finish current thread ASAP
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error sending interrupted");
            } catch (Exception e) {
                long nextBackOff = backOffExecution.nextBackOff();
                if (nextBackOff == BackOffExecution.STOP) {
                    log.error("Could not send error message to error handling service (" + attempt + ") and do not retry any more", e);
                    errorServiceFailedHandler.handle(e);
                    return;
                }

                //Sleep for one back off
                log.error("Could not send error message to error handling service (" + attempt + "), retry", e);
                try {
                    Thread.sleep(nextBackOff);
                } catch (InterruptedException e2) {
                    //In case of an interrupt finish current thread ASAP
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Error sending interrupted");
                }
                attempt++;
            }
        } while (true);
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<AvroMessageKey, AvroMessage> getKafkaTemplate(String clusterName) {
        String kafkaTemplateBeanName = jeapKafkaBeanNames.getKafkaTemplateBeanName(clusterName);
        return (KafkaTemplate<AvroMessageKey, AvroMessage>) beanFactory.getBean(kafkaTemplateBeanName);
    }

    private void logError(Object value, Exception e) {
        if (value instanceof Message msg) {
            String messageId = msg.getIdentity().getId();
            log.warn("{} cannot be consumed and will be sent to error service", keyValue("messageId", messageId), e);
        } else {
            log.warn("Got a non deserializable message that cannot be consumed and will be sent to error service", e);
        }
    }

    private MessageHandlerExceptionInformation getExceptionInformation(ConsumerRecord<?, ?> consumerRecord, Exception exception) {
        //If this is not a listener exception, there was something off
        if (!(exception instanceof ListenerExecutionFailedException listenerException)) {
            return MessageHandlerException.builder()
                    .description(null)
                    .errorCode(MessageHandlerExceptionInformation.StandardErrorCodes.UNKNOWN_EXCEPTION.name())
                    .temporality(MessageHandlerExceptionInformation.Temporality.UNKNOWN)
                    .cause(exception)
                    .build();
        }

        //Check if this was because it could not deserialize message
        if (consumerRecord.key() instanceof ErrorSerializedMessageHolder) {
            ErrorSerializedMessageHolder errorSerializedMessageHolder = (ErrorSerializedMessageHolder) consumerRecord.key();
            return createSerializationExceptionInformation(errorSerializedMessageHolder, "Could not deserialize key");
        }
        if (consumerRecord.value() instanceof ErrorSerializedMessageHolder) {
            ErrorSerializedMessageHolder errorSerializedMessageHolder = (ErrorSerializedMessageHolder) consumerRecord.value();
            return createSerializationExceptionInformation(errorSerializedMessageHolder, "Could not deserialize value");
        }

        //This was an exception in a listener, get the cause
        Throwable cause = listenerException.getCause();

        //Check if the cause was conversion error
        if (cause instanceof MessageConversionException) {
            return MessageHandlerException.builder()
                    .errorCode(MessageHandlerExceptionInformation.StandardErrorCodes.WRONG_EVENT_TYPE.name())
                    .temporality(MessageHandlerExceptionInformation.Temporality.PERMANENT)
                    .cause(cause)
                    .build();
        }

        //If not, treat this as exception in the handler
        return convertToValidExceptionInformation(cause);
    }

    private MessageHandlerExceptionInformation convertToValidExceptionInformation(Throwable cause) {
        //If cause is not event an exception information, return a general information
        if (!(cause instanceof MessageHandlerExceptionInformation inf)) {
            return MessageHandlerException.builder()
                    .errorCode(MessageHandlerExceptionInformation.StandardErrorCodes.UNKNOWN_EXCEPTION.name())
                    .temporality(MessageHandlerExceptionInformation.Temporality.UNKNOWN)
                    .cause(cause)
                    .build();
        }

        //Otherwise check if information is valid
        if (inf.getErrorCode() != null && inf.getMessage() != null && inf.getTemporality() != null) {
            log.debug("errorCode {}, message {}, temporality {} set, returning exception to builder", inf.getErrorCode(), inf.getMessage(), inf.getTemporality());
            return inf;
        }

        log.error("Got an invalid exception information. Please fix your code!");
        log.error("errorCode {}, message {} or temporality {} are missing in the exception", inf.getErrorCode(), inf.getMessage(), inf.getTemporality());

        //If information is not valid, try to keep as much as possible and replace remaining with default
        return MessageHandlerException.builder()
                .description(inf.getDescription())
                .errorCode(inf.getErrorCode() != null ? inf.getErrorCode() : MessageHandlerExceptionInformation.StandardErrorCodes.INVALID_EXCEPTION.name())
                .temporality(inf.getTemporality() != null ? inf.getTemporality() : MessageHandlerExceptionInformation.Temporality.UNKNOWN)
                .cause(cause)
                .build();
    }
}
