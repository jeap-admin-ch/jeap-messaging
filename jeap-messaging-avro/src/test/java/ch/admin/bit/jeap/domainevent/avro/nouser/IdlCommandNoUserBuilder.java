package ch.admin.bit.jeap.domainevent.avro.nouser;

import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.domainevent.avro.command.idl.*;
import lombok.Getter;

@Getter
class IdlCommandNoUserBuilder extends AvroCommandBuilder<IdlCommandNoUserBuilder, IdlTestNoUserCommand> {
    private final String systemName = "DomainEventTest";
    private final String serviceName = "IdlTest";
    private final String specifiedMessageTypeVersion = "1.0.0";

    private IdlCommandNoUserBuilder() {
        super(IdlTestNoUserCommand::new);
    }

    static IdlCommandNoUserBuilder create() {
        return new IdlCommandNoUserBuilder();
    }

    @Override
    protected IdlCommandNoUserBuilder self() {
        return this;
    }

    @Override
    public IdlTestNoUserCommand build() {
        final IdlTestNoUserReferences references = new IdlTestNoUserReferences();
        references.setCustomReference(MyCustomNoUserReference.newBuilder().setReferenceId("referenceId").setType("type").build());
        references.setOtherReference(MyOtherNoUserReference.newBuilder().setCustomId("customId").setType("type").build());
        setReferences(references);
        setPayload(new IdlTestNoUserPayload("test-message"));
        return super.build();
    }
}
