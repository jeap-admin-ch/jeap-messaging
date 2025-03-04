package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.MessageFilter;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;

public class TestMessageFilter implements MessageFilter<JmeDeclarationCreatedEvent> {

    @Override
    public boolean shouldSequence(JmeDeclarationCreatedEvent message) {
        return !"NOT_SEQUENCED".equals(message.getPayload().getMessage());
    }
}
