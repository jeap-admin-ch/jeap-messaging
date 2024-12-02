package ch.admin.bit.jeap.domainevent.avro.nouser;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.domainevent.avro.event.idl.*;
import lombok.Getter;

@Getter
class IdlEventNoUserBuilder extends AvroDomainEventBuilder<IdlEventNoUserBuilder, IdlTestNoUserEvent> {
    private final String systemName = "DomainEventTest";
    private final String serviceName = "IdlTest";
    private final String specifiedMessageTypeVersion = "1.0.0";

    private IdlEventNoUserBuilder() {
        super(IdlTestNoUserEvent::new);
    }

    static IdlEventNoUserBuilder create() {
        return new IdlEventNoUserBuilder();
    }

    @Override
    protected IdlEventNoUserBuilder self() {
        return this;
    }

    @Override
    public IdlTestNoUserEvent build() {
        final IdlTestNoUserReferences references = new IdlTestNoUserReferences();
        references.setCustomReference(MyCustomNoUserReference.newBuilder().setReferenceId("referenceId").setType("type").build());
        references.setOtherReference(MyOtherNoUserReference.newBuilder().setCustomId("customId").setType("type").build());
        setReferences(references);
        setPayload(new IdlTestNoUserPayload("test-message"));
        return super.build();
    }
}
