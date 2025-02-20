package ch.admin.bit.jeap.messaging.kafka.test.integration.common;


import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEventPayload;
import ch.admin.bit.jme.test.JmeSimpleTestEventReferences;

public class JmeSimpleTestEventBuilder extends AvroDomainEventBuilder<JmeSimpleTestEventBuilder, JmeSimpleTestEvent> {
    private String message;
    private String serviceName = "jeap-microservice-examples-kafka";

    private JmeSimpleTestEventBuilder() {
        super(JmeSimpleTestEvent::new);
    }

    public static JmeSimpleTestEventBuilder create() {
        return new JmeSimpleTestEventBuilder();
    }

    public JmeSimpleTestEventBuilder message(String message) {
        this.message = message;
        return this;
    }

    public JmeSimpleTestEventBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    @Override
    protected String getServiceName() {
        return serviceName;
    }

    @Override
    protected String getSystemName() {
        return "JME";
    }

    @Override
    protected JmeSimpleTestEventBuilder self() {
        return this;
    }

    @Override
    public JmeSimpleTestEvent build() {
        if (this.message == null) {
            throw AvroMessageBuilderException.propertyNull("jmeSimpleTestEventReferences.message");
        }
        JmeSimpleTestEventReferences references = JmeSimpleTestEventReferences.newBuilder()
                .build();
        JmeSimpleTestEventPayload payload = JmeSimpleTestEventPayload.newBuilder()
                .setMessage(message)
                .build();
        setReferences(references);
        setPayload(payload);
        return super.build();
    }


}
