package ch.admin.bit.jeap.domainevent;

import ch.admin.bit.jeap.messaging.model.Message;

public interface DomainEvent extends Message {
    String getDomainEventVersion();

    DomainEventIdentity getIdentity();
}
