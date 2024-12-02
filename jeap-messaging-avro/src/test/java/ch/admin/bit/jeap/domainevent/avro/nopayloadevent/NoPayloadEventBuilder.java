package ch.admin.bit.jeap.domainevent.avro.nopayloadevent;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.domainevent.avro.event.nopayload.MyCustomReference;
import ch.admin.bit.jeap.domainevent.avro.event.nopayload.NoPayloadTestEvent;
import ch.admin.bit.jeap.domainevent.avro.event.nopayload.NoPayloadTestReferences;
import lombok.Getter;

@Getter
class NoPayloadEventBuilder extends AvroDomainEventBuilder<NoPayloadEventBuilder, NoPayloadTestEvent> {
    private final String systemName = "DomainEventTest";
    private final String serviceName = "NoPayloadTest";
    private final String specifiedMessageTypeVersion = "1.0.0";

    private NoPayloadEventBuilder() {
        super(NoPayloadTestEvent::new);
    }

    static NoPayloadEventBuilder create() {
        return new NoPayloadEventBuilder();
    }

    @Override
    protected NoPayloadEventBuilder self() {
        return this;
    }

    @Override
    public NoPayloadTestEvent build() {
        setReferences(NoPayloadTestReferences.newBuilder()
                .setReference1(MyCustomReference.newBuilder().setReferenceId("referenceId").setType("type").build())
                .build());
        return super.build();
    }

}
