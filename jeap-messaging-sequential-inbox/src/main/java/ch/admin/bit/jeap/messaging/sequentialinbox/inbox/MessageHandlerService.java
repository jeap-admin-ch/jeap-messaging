package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.interceptor.Callbacks;
import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.MessageHandlerProvider;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
class MessageHandlerService {

    private static final DefaultTransactionDefinition NOT_SUPPORTED = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);

    private final MessageHandlerProvider messageHandlerProvider;
    private final PlatformTransactionManager platformTransactionManager;
    private final List<JeapKafkaMessageCallback> callbacks;

    void handle(DeserializedMessage deserializedMessage) {
        String jeapMessageTypeName = deserializedMessage.message().getType().getName();
        SequentialInboxMessageHandler messageHandler = messageHandlerProvider.getHandlerForJeapMessageType(jeapMessageTypeName);
        invokeMessageHandler(deserializedMessage, messageHandler);
    }

    private void invokeMessageHandler(DeserializedMessage deserializedMessage, SequentialInboxMessageHandler messageHandler) {
        invokeMessageHandler(deserializedMessage.key(), deserializedMessage.message(), deserializedMessage.topicName(), messageHandler);
    }

    void invokeMessageHandler(AvroMessageKey key, AvroMessage message, String topicName, SequentialInboxMessageHandler messageHandler) {
        callbacks.forEach(callback -> Callbacks.invokeCallback(message, topicName, callback::beforeConsume));
        try {
            doInvokeMessageHandler(key, message, messageHandler);
            callbacks.forEach(callback -> Callbacks.invokeCallback(message, topicName, callback::afterConsume));
        } finally {
            callbacks.forEach(callback -> Callbacks.invokeCallback(message, topicName, callback::afterRecord));
        }
    }

    private void doInvokeMessageHandler(AvroMessageKey key, AvroMessage message, SequentialInboxMessageHandler messageHandler) {
        // NOT_SUPPORTED will suspend the current transaction and avoid "leaking" the transaction to the application code.
        // The application code can control its own transactions independently of the sequenced inbox transaction.
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager, NOT_SUPPORTED);
        transactionTemplate.executeWithoutResult(ignored -> messageHandler.invoke(key, message));
    }
}
