package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message;

import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxMessageListener;
import ch.admin.bit.jme.test.JmeEnumTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

@Component
@RequiredArgsConstructor
public class MultipleTestEventListener {

    private final MessageRecorder messageRecorder;

    public static boolean failOnJmeSimpleTestEvent = false;

    @SequentialInboxMessageListener
    @Transactional
    public void onEvent(AvroMessageKey key, JmeSimpleTestEvent message) {
        if (failOnJmeSimpleTestEvent) {
            throw new RuntimeException("Failing on JmeSimpleTestEvent");
        }

        messageRecorder.recordMessage(key, message);

        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .describedAs("Transaction should be active due to the @Transactional annotation")
                .isTrue();
    }

    // Test one SequentialInboxMessageListener without a key argument
    @SequentialInboxMessageListener
    public void onEvent(JmeEnumTestEvent message) {
        messageRecorder.recordMessage(message);

        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .describedAs("No transaction should be active in the listener method - " +
                        "the sequential inbox should suspend the transaction while invoking the listener")
                .isFalse();
    }
}
