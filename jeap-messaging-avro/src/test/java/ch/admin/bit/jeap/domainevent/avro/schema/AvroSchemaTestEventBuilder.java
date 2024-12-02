package ch.admin.bit.jeap.domainevent.avro.schema;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.domainevent.avro.event.schema.AvroSchemaTestEvent;
import ch.admin.bit.jeap.domainevent.avro.event.schema.AvroSchemaTestPayload;
import ch.admin.bit.jeap.domainevent.avro.event.schema.AvroSchemaTestReferences;
import ch.admin.bit.jeap.domainevent.avro.event.schema.TestReference;
import lombok.Getter;

@Getter
class AvroSchemaTestEventBuilder extends AvroDomainEventBuilder<AvroSchemaTestEventBuilder, AvroSchemaTestEvent> {
    private final String systemName = "DomainEventTest";
    private final String serviceName = "AvroSchemaTestEventTest";
    private final String specifiedMessageTypeVersion = "1.0.0";

    private AvroSchemaTestEventBuilder() {
        super(AvroSchemaTestEvent::new);
    }

    static AvroSchemaTestEventBuilder create() {
        return new AvroSchemaTestEventBuilder();
    }

    @Override
    protected AvroSchemaTestEventBuilder self() {
        return this;
    }

    @Override
    public AvroSchemaTestEvent build() {

        setReferences(
                AvroSchemaTestReferences.newBuilder()
                        .setReference1(TestReference.newBuilder().setTestId("testId").setType("type").build())
                        .build());
        setPayload(new AvroSchemaTestPayload("test-message"));
        return super.build();
    }

}
