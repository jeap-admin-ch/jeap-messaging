package ch.admin.bit.jeap.domainevent.avro.idlevent;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.domainevent.avro.event.idl.*;
import lombok.Getter;

@Getter
class IdlEventBuilder extends AvroDomainEventBuilder<IdlEventBuilder, IdlTestEvent> {
    private final String systemName = "DomainEventTest";
    private final String serviceName = "IdlTest";
    private final String specifiedMessageTypeVersion = "1.0.0";

    private IdlEventBuilder() {
        super(IdlTestEvent::new);
    }

    static IdlEventBuilder create() {
        return new IdlEventBuilder();
    }

    @Override
    protected IdlEventBuilder self() {
        return this;
    }

    @Override
    public IdlTestEvent build() {
        final IdlTestReferences references = new IdlTestReferences();
        references.setCustomReference(MyCustomReference.newBuilder().setReferenceId("referenceId").setType("type").build());
        references.setOtherReference(MyOtherReference.newBuilder().setCustomId("customId").setType("type").build());
        setReferences(references);
        setPayload(new IdlTestPayload("test-message"));
        return super.build();
    }
}
