package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.MessageHandlerProvider;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
class MessageHandlerService {

    private static final DefaultTransactionDefinition NOT_SUPPORTED = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);

    private final MessageHandlerProvider messageHandlerProvider;
    private final PlatformTransactionManager platformTransactionManager;

    void handle(DeserializedMessage deserializedMessage) {
        SequentialInboxMessageHandler messageHandler = messageHandlerProvider.getHandlerForMessageType(deserializedMessage.messageType());
        invokeMessageHandler(deserializedMessage, messageHandler);
    }

    private void invokeMessageHandler(DeserializedMessage deserializedMessage, SequentialInboxMessageHandler messageHandler) {
        invokeMessageHandler(deserializedMessage.key(), deserializedMessage.message(), messageHandler);
    }

    void invokeMessageHandler(AvroMessageKey key, AvroMessage message, SequentialInboxMessageHandler messageHandler) {
        // NOT_SUPPORTED will suspend the current transaction and avoid "leaking" the transaction to the application code.
        // The application code can control its own transactions independently of the sequenced inbox transaction.
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager, NOT_SUPPORTED);
        transactionTemplate.executeWithoutResult(ignored -> messageHandler.invoke(key, message));
    }

}
