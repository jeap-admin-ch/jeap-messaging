package ch.admin.bit.jeap.domainevent.avro;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;

public interface AvroDomainEvent extends AvroMessage, DomainEvent {
    void setDomainEventVersion(String domainEventVersion);

    void setIdentity(AvroDomainEventIdentity identity);

    void setPublisher(AvroDomainEventPublisher publisher);

    void setType(AvroDomainEventType type);

    default void setUser(AvroDomainEventUser user) {
        throw AvroMessageBuilderException.userFieldNotDefined(this.getClass());
    }

}

