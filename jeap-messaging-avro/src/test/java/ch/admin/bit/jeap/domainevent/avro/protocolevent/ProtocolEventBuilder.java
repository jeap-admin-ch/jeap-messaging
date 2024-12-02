package ch.admin.bit.jeap.domainevent.avro.protocolevent;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.domainevent.avro.event.protocol.AvroProtocolTestEvent;
import ch.admin.bit.jeap.domainevent.avro.event.protocol.AvroProtocolTestPayload;
import ch.admin.bit.jeap.domainevent.avro.event.protocol.AvroProtocolTestReferences;
import ch.admin.bit.jeap.domainevent.avro.event.protocol.TestReference;
import lombok.Getter;

@Getter
class ProtocolEventBuilder extends AvroDomainEventBuilder<ProtocolEventBuilder, AvroProtocolTestEvent> {
    private final String systemName = "DomainEventTest";
    private final String serviceName = "ProtocolTest";
    private final String specifiedMessageTypeVersion = "1.0.0";

    private ProtocolEventBuilder() {
        super(AvroProtocolTestEvent::new);
    }

    static ProtocolEventBuilder create() {
        return new ProtocolEventBuilder();
    }

    @Override
    protected ProtocolEventBuilder self() {
        return this;
    }

    @Override
    public AvroProtocolTestEvent build() {
        setReferences(AvroProtocolTestReferences.newBuilder()
                .setReference1(TestReference.newBuilder().setTestId("testId").setType("type").build())
                .build());
        setPayload(new AvroProtocolTestPayload("test-message"));
        return super.build();
    }
}
