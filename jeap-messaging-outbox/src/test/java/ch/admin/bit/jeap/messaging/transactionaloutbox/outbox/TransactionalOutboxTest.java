package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.contract.NoContractException;
import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.model.MessageType;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.testsupport.StringMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class TransactionalOutboxTest {

    @Mock
    MessageSerializer serializer;
    @Mock
    DeferredMessageRepository deferredMessageRepository;
    @Mock
    FailedMessageRepository failedMessageRepository;
    @Mock
    AfterCommitMessageSender afterCommitMessageSender;
    @Mock
    ContractsValidator contractsValidator;
    @Mock
    OutboxTracing outboxTracing;
    @Mock
    DeferredMessage deferredMessage;
    @Mock
    JeapKafkaMessageCallback callback;

    TransactionalOutbox transactionalOutbox;

    @BeforeEach
    void setUp() {
        transactionalOutbox = new TransactionalOutbox(KafkaProperties.DEFAULT_CLUSTER, serializer,
                deferredMessageRepository, failedMessageRepository, afterCommitMessageSender,
                contractsValidator, Optional.empty(), outboxTracing, List.of(callback));
    }

    @Test
    void testSend_WhenContractValidationFails_ThenTransactionalOutboxExceptionThrown() {
        Mockito.doThrow(NoContractException.class).when(contractsValidator).ensurePublisherContract(any(MessageType.class), anyString());
        final StringMessage testMessage = StringMessage.from("test-message");

        assertThatThrownBy(() -> transactionalOutbox.sendMessage(testMessage, "topic")).isInstanceOf(TransactionalOutboxException.class);
        assertThatThrownBy(() -> transactionalOutbox.sendMessageScheduled(testMessage, "topic")).isInstanceOf(TransactionalOutboxException.class);
        assertThatThrownBy(() -> transactionalOutbox.sendMessage(testMessage, "key", "topic")).isInstanceOf(TransactionalOutboxException.class);
        assertThatThrownBy(() -> transactionalOutbox.sendMessageScheduled(testMessage, "key", "topic")).isInstanceOf(TransactionalOutboxException.class);
    }

    @Test
    void testSend_callbackInvokedAfterSend() {
        final StringMessage testMessage = StringMessage.from("test-message");
        doReturn(new byte[0]).when(serializer).serializeMessage(any(), any());
        doReturn(deferredMessage).when(deferredMessageRepository).save(any());

        transactionalOutbox.sendMessage(testMessage, "topic");

        verify(callback).onSend(testMessage, "topic");
        verifyNoMoreInteractions(callback);
    }
}
