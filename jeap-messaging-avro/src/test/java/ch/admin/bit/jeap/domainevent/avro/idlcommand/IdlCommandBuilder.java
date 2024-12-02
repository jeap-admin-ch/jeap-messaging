package ch.admin.bit.jeap.domainevent.avro.idlcommand;

import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.domainevent.avro.command.idl.*;
import lombok.Getter;

@Getter
class IdlCommandBuilder extends AvroCommandBuilder<IdlCommandBuilder, IdlTestCommand> {
    private final String systemName = "DomainEventTest";
    private final String serviceName = "IdlTest";
    private final String specifiedMessageTypeVersion = "1.0.0";

    private IdlCommandBuilder() {
        super(IdlTestCommand::new);
    }

    static IdlCommandBuilder create() {
        return new IdlCommandBuilder();
    }

    @Override
    protected IdlCommandBuilder self() {
        return this;
    }

    @Override
    public IdlTestCommand build() {
        final IdlTestReferences references = new IdlTestReferences();
        references.setCustomReference(MyCustomReference.newBuilder().setReferenceId("referenceId").setType("type").build());
        references.setOtherReference(MyOtherReference.newBuilder().setCustomId("customId").setType("type").build());
        setReferences(references);
        setPayload(new IdlTestPayload("test-message"));
        return super.build();
    }
}
