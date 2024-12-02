package ch.admin.bit.jeap.domainevent;

import ch.admin.bit.jeap.messaging.model.MessageIdentity;

public interface DomainEventIdentity extends MessageIdentity {
    default String getId() {
        return getEventId();
    }

    String getEventId();
}
