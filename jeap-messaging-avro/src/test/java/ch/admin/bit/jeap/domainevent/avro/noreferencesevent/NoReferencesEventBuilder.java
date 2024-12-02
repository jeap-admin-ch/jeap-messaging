package ch.admin.bit.jeap.domainevent.avro.noreferencesevent;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.domainevent.avro.event.noreferencesevent.NoReferencesEvent;
import lombok.Getter;

@Getter
class NoReferencesEventBuilder extends AvroDomainEventBuilder<NoReferencesEventBuilder, NoReferencesEvent> {
    private final String systemName = "DomainEventTest";
    private final String serviceName = "NoReferencesTest";
    private final String specifiedMessageTypeVersion = "1.0.0";

    private NoReferencesEventBuilder() {
        super(NoReferencesEvent::new);
    }

    static NoReferencesEventBuilder create() {
        return new NoReferencesEventBuilder();
    }

    @Override
    protected NoReferencesEventBuilder self() {
        return this;
    }

    @Override
    public NoReferencesEvent build() {
        return super.build();
    }
}
