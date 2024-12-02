package ch.admin.bit.jeap.messaging.idempotence.testsupport;

import ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandler;
import ch.admin.bit.jeap.messaging.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestMessageHandler {

    private final Callback callback;

    @IdempotentMessageHandler
    public void handleMessage(Message message) {
        callback.handling(message.getIdentity().getIdempotenceId(), message.getType().getName());
    }

    @IdempotentMessageHandler
    public void handleMessageWithAdditionalParameters(Message message, int value, String text) {
        callback.handling(message.getIdentity().getIdempotenceId(), message.getType().getName());
    }

    public interface Callback {
        void handling(String idempotenceId, String messageTypeName);
    }

}
