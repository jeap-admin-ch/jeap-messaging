package ch.admin.bit.jeap.domainevent.avro.idlemptyreference;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.domainevent.avro.event.idlemptyreferences.IdlTestEmptyReferencesEvent;
import ch.admin.bit.jeap.domainevent.avro.event.idlemptyreferences.IdlTestEmptyReferencesPayload;
import ch.admin.bit.jeap.domainevent.avro.event.idlemptyreferences.IdlTestEmptyReferencesReferences;
import lombok.Getter;

@Getter
class IdlTestEmptyReferencesEventBuilder extends AvroDomainEventBuilder<IdlTestEmptyReferencesEventBuilder, IdlTestEmptyReferencesEvent> {
    private final String systemName = "DomainEventTest";
    private final String serviceName = "IdlEmptyReferencesTest";
    private final String specifiedMessageTypeVersion = "1.0.0";

    private IdlTestEmptyReferencesEventBuilder() {
        super(IdlTestEmptyReferencesEvent::new);
    }

    static IdlTestEmptyReferencesEventBuilder create() {
        return new IdlTestEmptyReferencesEventBuilder();
    }

    @Override
    protected IdlTestEmptyReferencesEventBuilder self() {
        return this;
    }

    @Override
    public IdlTestEmptyReferencesEvent build() {
        setReferences(new IdlTestEmptyReferencesReferences());
        setPayload(new IdlTestEmptyReferencesPayload("test-message"));
        return super.build();
    }
}
