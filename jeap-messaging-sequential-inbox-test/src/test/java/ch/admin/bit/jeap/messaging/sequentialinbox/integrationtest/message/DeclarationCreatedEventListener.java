package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message;

import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxMessageListener;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DeclarationCreatedEventListener {
    public static final String FAILURE = "FAILURE";

    private final MessageRecorder messageRecorder;

    @SequentialInboxMessageListener
    public void onEvent(AvroMessageKey key, JmeDeclarationCreatedEvent message) {
        if (FAILURE.equals(message.getPayload().getMessage())) {
            throw new RuntimeException(message.getPayload().getMessage());
        }
        messageRecorder.recordMessage(key, message);
    }
}
